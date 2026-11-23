package com.team.itexperts.shrinkflation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.calc.ShrinkflationCalculator
import com.team.itexperts.shrinkflation.model.ShrinkflationResult
import com.team.itexperts.shrinkflation.data.PriceEntryEntity
import com.team.itexperts.shrinkflation.data.PriceEntryRepository
import com.team.itexperts.shrinkflation.data.ProductCard
import com.team.itexperts.shrinkflation.data.ProductRepository
import com.team.itexperts.shrinkflation.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class ShrinkUiState(
    val priceOld: String = "",
    val qtyOld: String = "",
    val unitType: UnitType = UnitType.MASS,
    val massUnitOld: MassUnit = MassUnit.G,
    val volumeUnitOld: VolumeUnit = VolumeUnit.ML,
    val priceNew: String = "",
    val qtyNew: String = "",
    val massUnitNew: MassUnit = MassUnit.G,
    val volumeUnitNew: VolumeUnit = VolumeUnit.ML,
    val result: ShrinkflationResult? = null,
    val error: String? = null,
    val productName: String = "",
    val brand: String = "",
    val spec: String = "",
    val saveProduct: Boolean = true,
    val savePricesEnabled: Boolean = true,
    val currency: Currency = Currency.RUB,
    val suggestions: List<ProductCard> = emptyList(),
    val searchField: String? = null
)

@HiltViewModel
class ShrinkflationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productRepo: ProductRepository,
    private val priceRepo: PriceEntryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ShrinkUiState())
    val state: StateFlow<ShrinkUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun update(transform: (ShrinkUiState) -> ShrinkUiState) { _state.value = transform(_state.value) }

    fun reset() { _state.value = ShrinkUiState(currency = _state.value.currency, savePricesEnabled = _state.value.savePricesEnabled) }

    fun swap() {
        val s = _state.value
        _state.value = s.copy(
            priceOld = s.priceNew, qtyOld = s.qtyNew, massUnitOld = s.massUnitNew, volumeUnitOld = s.volumeUnitNew,
            priceNew = s.priceOld, qtyNew = s.qtyOld, massUnitNew = s.massUnitOld, volumeUnitNew = s.volumeUnitOld
        )
    }

    fun calculate() {
        val s = _state.value
        try {
            val old = PackageInput(
                price = BigDecimal(s.priceOld), quantity = BigDecimal(s.qtyOld),
                unitType = s.unitType, massUnit = s.massUnitOld, volumeUnit = s.volumeUnitOld
            )
            val new = PackageInput(
                price = BigDecimal(s.priceNew), quantity = BigDecimal(s.qtyNew),
                unitType = s.unitType, massUnit = s.massUnitNew, volumeUnit = s.volumeUnitNew
            )
            val res = ShrinkflationCalculator.calculate(old, new)
            _state.value = s.copy(result = res, error = null)
            if (s.savePricesEnabled && s.saveProduct && s.productName.isNotBlank()) {
                viewModelScope.launch { savePriceRecords(s) }
            }
        } catch (e: Exception) {
            _state.value = s.copy(result = null, error = e.message ?: context.getString(R.string.error_input))
        }
    }

    private suspend fun savePriceRecords(s: ShrinkUiState) {
        val product = productRepo.findOrCreateProduct(s.productName, s.unitType)
        val variant = productRepo.findOrCreateVariant(product.id, s.brand.ifBlank { null }, s.spec.ifBlank { null })
        val since = System.currentTimeMillis() - 5 * 60 * 1000
        val existing = priceRepo.findRecentDuplicate(variant.id, s.priceNew, s.qtyNew, s.unitType.name, since)
        if (existing == null) {
            priceRepo.insert(PriceEntryEntity(
                variantId = variant.id,
                price = s.priceNew,
                quantity = s.qtyNew,
                unit = s.unitType.name,
                currency = s.currency,
                source = "shrinkflation-screen"
            ))
        }
    }

    fun onProductNameChange(v: String) {
        _state.value = _state.value.copy(productName = v, searchField = "name")
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val list = ProductSearch.search(productRepo, "name", v)
            _state.value = _state.value.copy(suggestions = list, searchField = if (v.isBlank()) null else "name")
        }
    }

    fun onBrandChange(v: String) {
        _state.value = _state.value.copy(brand = v, searchField = "brand")
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val list = ProductSearch.search(productRepo, "brand", v)
            _state.value = _state.value.copy(suggestions = list, searchField = if (v.isBlank()) null else "brand")
        }
    }

    fun onSpecChange(v: String) {
        _state.value = _state.value.copy(spec = v, searchField = "spec")
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val list = ProductSearch.search(productRepo, "spec", v)
            _state.value = _state.value.copy(suggestions = list, searchField = if (v.isBlank()) null else "spec")
        }
    }

    fun selectProduct(card: ProductCard, field: String?) {
        val cur = _state.value
        val (n, b, s) = ProductSearch.applySelection(card, field, cur.productName, cur.brand, cur.spec)
        _state.value = cur.copy(
            productName = n,
            brand = b,
            spec = s,
            unitType = card.unitType,
            suggestions = emptyList(),
            searchField = null
        )
    }
}
