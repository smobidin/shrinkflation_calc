package com.team.itexperts.shrinkflation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.calc.UnitConverter
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

data class UnitPriceUiState(
    val productName: String = "",
    val brand: String = "",
    val spec: String = "",
    val saveProduct: Boolean = true,
    val price: String = "",
    val qty: String = "",
    val unitType: UnitType = UnitType.MASS,
    val massUnit: MassUnit = MassUnit.G,
    val volumeUnit: VolumeUnit = VolumeUnit.ML,
    val result: BigDecimal? = null,
    val error: String? = null,
    val savePricesEnabled: Boolean = true,
    val currency: Currency = Currency.RUB,
    val suggestions: List<ProductCard> = emptyList(),
    val searchField: String? = null
)

@HiltViewModel
class UnitPriceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productRepo: ProductRepository,
    private val priceRepo: PriceEntryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UnitPriceUiState())
    val state: StateFlow<UnitPriceUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun update(transform: (UnitPriceUiState) -> UnitPriceUiState) { _state.value = transform(_state.value) }

    fun reset() { _state.value = UnitPriceUiState(currency = _state.value.currency, savePricesEnabled = _state.value.savePricesEnabled) }

    fun calculate() {
        val s = _state.value
        try {
            val input = PackageInput(
                price = BigDecimal(s.price), quantity = BigDecimal(s.qty),
                unitType = s.unitType, massUnit = s.massUnit, volumeUnit = s.volumeUnit
            )
            val up = UnitConverter.unitPriceWhole(input)
            _state.value = s.copy(result = up, error = null)
            if (s.savePricesEnabled && s.saveProduct && s.productName.isNotBlank()) {
                viewModelScope.launch { savePrice(s) }
            }
        } catch (e: Exception) {
            _state.value = s.copy(result = null, error = e.message ?: context.getString(R.string.error_input))
        }
    }

    private suspend fun savePrice(s: UnitPriceUiState) {
        val product = productRepo.findOrCreateProduct(s.productName, s.unitType)
        val variant = productRepo.findOrCreateVariant(product.id, s.brand.ifBlank { null }, s.spec.ifBlank { null })
        val since = System.currentTimeMillis() - 5 * 60 * 1000
        val existing = priceRepo.findRecentDuplicate(variant.id, s.price, s.qty, s.unitType.name, since)
        if (existing == null) {
            priceRepo.insert(PriceEntryEntity(
                variantId = variant.id,
                price = s.price,
                quantity = s.qty,
                unit = s.unitType.name,
                currency = s.currency,
                source = "unit-price-screen"
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
            tryPrefill()
        }
    }

    fun onBrandChange(v: String) {
        _state.value = _state.value.copy(brand = v, searchField = "brand")
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val list = ProductSearch.search(productRepo, "brand", v)
            _state.value = _state.value.copy(suggestions = list, searchField = if (v.isBlank()) null else "brand")
            tryPrefill()
        }
    }

    fun onSpecChange(v: String) {
        _state.value = _state.value.copy(spec = v, searchField = "spec")
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val list = ProductSearch.search(productRepo, "spec", v)
            _state.value = _state.value.copy(suggestions = list, searchField = if (v.isBlank()) null else "spec")
            tryPrefill()
        }
    }

    private fun tryPrefill() {
        val s = _state.value
        if (s.productName.isBlank() || s.brand.isBlank() || s.spec.isBlank()) return
        if (s.price.isNotBlank() || s.qty.isNotBlank()) return
        viewModelScope.launch {
            val product = productRepo.findOrCreateProduct(s.productName, s.unitType)
            val variant = productRepo.findOrCreateVariant(product.id, s.brand.ifBlank { null }, s.spec.ifBlank { null })
            val last = priceRepo.getLastByVariant(variant.id)
            if (last != null) {
                val fresh = _state.value
                if (fresh.price.isBlank() && fresh.qty.isBlank()) {
                    _state.value = fresh.copy(price = last.price, qty = last.quantity)
                }
            }
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
        tryPrefill()
    }
}
