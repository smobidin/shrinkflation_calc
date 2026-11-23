package com.team.itexperts.shrinkflation.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.data.CatalogRepository
import com.team.itexperts.shrinkflation.data.CatalogRow
import com.team.itexperts.shrinkflation.data.PriceEntryEntity
import com.team.itexperts.shrinkflation.data.PriceEntryRepository
import com.team.itexperts.shrinkflation.data.ProductRepository
import com.team.itexperts.shrinkflation.model.*
import com.team.itexperts.shrinkflation.storage.Keys
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CatalogEdit(
    val key: Long,
    val productId: Long,
    val variantId: Long?,
    val name: String,
    val brand: String,
    val spec: String,
    val unitType: UnitType,
    val massUnit: MassUnit,
    val volumeUnit: VolumeUnit,
    val price: String,
    val quantity: String,
    val error: String? = null
)

data class CatalogState(
    val all: List<CatalogRow> = emptyList(),
    val typeFilter: UnitType? = null,
    val brandFilter: String = "",
    val specFilter: String = "",
    val brandSuggestions: List<String> = emptyList(),
    val specSuggestions: List<String> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val editing: CatalogEdit? = null,
    val error: String? = null,
    val currency: Currency = Currency.RUB
) {
    val visible: List<CatalogRow> get() = all.filter { row ->
        (typeFilter == null || row.unitType == typeFilter) &&
            (brandFilter.isBlank() || row.brand?.contains(brandFilter, ignoreCase = true) == true) &&
            (specFilter.isBlank() || row.spec?.contains(specFilter, ignoreCase = true) == true)
    }

    val selectedRows: List<CatalogRow> get() = all.filter { it.key in selected }
}

private fun unitsOf(row: CatalogRow): Pair<MassUnit, VolumeUnit> = when (row.unit) {
    MassUnit.KG.name -> MassUnit.KG to VolumeUnit.ML
    VolumeUnit.L.name -> MassUnit.G to VolumeUnit.L
    VolumeUnit.ML.name -> MassUnit.G to VolumeUnit.ML
    else -> MassUnit.G to VolumeUnit.ML
}

@HiltViewModel
class CatalogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val catalogRepo: CatalogRepository,
    private val productRepo: ProductRepository,
    private val priceRepo: PriceEntryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private var brandJob: Job? = null
    private var specJob: Job? = null

    init {
        viewModelScope.launch {
            catalogRepo.catalog().collect { rows ->
                _state.value = _state.value.copy(all = rows)
            }
        }
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                prefs[Keys.CURRENCY]?.let { runCatching { Currency.valueOf(it) }.getOrNull() } ?: Currency.RUB
            }.collect { currency ->
                _state.value = _state.value.copy(currency = currency)
            }
        }
    }

    fun setTypeFilter(type: UnitType?) {
        _state.value = _state.value.copy(typeFilter = type)
    }

    fun setBrandFilter(value: String) {
        _state.value = _state.value.copy(brandFilter = value)
        brandJob?.cancel()
        brandJob = viewModelScope.launch {
            delay(300)
            if (value.isBlank()) {
                _state.value = _state.value.copy(brandSuggestions = emptyList())
            } else {
                _state.value = _state.value.copy(
                    brandSuggestions = productRepo.searchByBrand(value).mapNotNull { it.brand }.distinct()
                )
            }
        }
    }

    fun selectBrandSuggestion(value: String) {
        brandJob?.cancel()
        _state.value = _state.value.copy(brandFilter = value, brandSuggestions = emptyList())
    }

    fun setSpecFilter(value: String) {
        _state.value = _state.value.copy(specFilter = value)
        specJob?.cancel()
        specJob = viewModelScope.launch {
            delay(300)
            if (value.isBlank()) {
                _state.value = _state.value.copy(specSuggestions = emptyList())
            } else {
                _state.value = _state.value.copy(
                    specSuggestions = productRepo.searchBySpec(value).mapNotNull { it.spec }.distinct()
                )
            }
        }
    }

    fun selectSpecSuggestion(value: String) {
        specJob?.cancel()
        _state.value = _state.value.copy(specFilter = value, specSuggestions = emptyList())
    }

    fun toggle(key: Long) {
        val s = _state.value
        _state.value = s.copy(
            selected = if (key in s.selected) s.selected - key
            else if (s.selected.size < 4) s.selected + key
            else s.selected
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = emptySet())
    }

    fun startEdit(row: CatalogRow) {
        val (mass, volume) = unitsOf(row)
        _state.value = _state.value.copy(
            editing = CatalogEdit(
                key = row.key,
                productId = row.productId,
                variantId = row.variantId,
                name = row.name,
                brand = row.brand ?: "",
                spec = row.spec ?: "",
                unitType = row.unitType,
                massUnit = mass,
                volumeUnit = volume,
                price = row.price ?: "",
                quantity = row.quantity ?: ""
            ),
            error = null
        )
    }

    fun updateEdit(transform: (CatalogEdit) -> CatalogEdit) {
        val current = _state.value.editing ?: return
        _state.value = _state.value.copy(editing = transform(current))
    }

    fun cancelEdit() {
        _state.value = _state.value.copy(editing = null)
    }

    fun saveEdit() {
        val edit = _state.value.editing ?: return
        if (edit.name.isBlank()) {
            _state.value = _state.value.copy(
                editing = edit.copy(error = context.getString(R.string.catalog_error_name))
            )
            return
        }
        val price = edit.price.toBigDecimalOrNull()
        val qty = edit.quantity.toBigDecimalOrNull()
        if (price == null || qty == null || price <= BigDecimal.ZERO || qty <= BigDecimal.ZERO) {
            _state.value = _state.value.copy(
                editing = edit.copy(error = context.getString(R.string.catalog_error_price))
            )
            return
        }
        viewModelScope.launch {
            val product = productRepo.getProduct(edit.productId)
            if (product == null) {
                _state.value = _state.value.copy(error = context.getString(R.string.catalog_error_save))
                return@launch
            }
            val brand = edit.brand.ifBlank { null }
            val spec = edit.spec.ifBlank { null }
            val conflict = productRepo.variantConflict(edit.productId, brand, spec, edit.variantId)
            if (conflict != null) {
                _state.value = _state.value.copy(
                    editing = edit.copy(error = context.getString(R.string.catalog_error_duplicate))
                )
                return@launch
            }

            productRepo.updateProduct(product.copy(name = edit.name, unitType = edit.unitType))
            val variantId = if (edit.variantId != null) {
                productRepo.getVariant(edit.variantId)?.let { variant ->
                    productRepo.updateVariant(variant.copy(brand = brand, spec = spec))
                    variant.id
                }
            } else if (brand != null || spec != null) {
                productRepo.findOrCreateVariant(product.id, brand, spec).id
            } else null

            val unit = when (edit.unitType) {
                UnitType.MASS -> edit.massUnit.name
                UnitType.VOLUME -> edit.volumeUnit.name
                UnitType.COUNT -> UnitType.COUNT.name
            }
            priceRepo.insert(
                PriceEntryEntity(
                    variantId = variantId,
                    price = edit.price,
                    quantity = edit.quantity,
                    unit = unit,
                    currency = _state.value.currency,
                    source = "catalog-edit"
                )
            )
            _state.value = _state.value.copy(editing = null, error = null)
        }
    }

    fun delete(row: CatalogRow) {
        viewModelScope.launch {
            catalogRepo.deleteRow(row)
            _state.value = _state.value.copy(selected = _state.value.selected - row.key)
        }
    }

    fun compare(selectionVm: ComparisonSelectionViewModel) {
        val rows = _state.value.selectedRows
        if (rows.size !in 2..4) return
        selectionVm.submit(rows.map { row ->
            val (mass, volume) = unitsOf(row)
            CompInput(
                name = row.name,
                brand = row.brand ?: "",
                spec = row.spec ?: "",
                price = row.price ?: "",
                qty = row.quantity ?: "",
                unitType = row.unitType,
                massUnit = mass,
                volumeUnit = volume
            )
        })
    }
}
