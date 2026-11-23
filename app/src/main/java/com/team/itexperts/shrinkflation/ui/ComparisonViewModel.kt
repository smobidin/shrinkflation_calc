package com.team.itexperts.shrinkflation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.calc.ComparisonCalculator
import com.team.itexperts.shrinkflation.calc.ComparisonItem
import com.team.itexperts.shrinkflation.calc.ComparisonResult
import com.team.itexperts.shrinkflation.data.ProductCard
import com.team.itexperts.shrinkflation.data.PriceEntryEntity
import com.team.itexperts.shrinkflation.data.PriceEntryRepository
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

data class CompInput(
    val name: String = "",
    val brand: String = "",
    val spec: String = "",
    val price: String = "",
    val qty: String = "",
    val unitType: UnitType = UnitType.MASS,
    val massUnit: MassUnit = MassUnit.G,
    val volumeUnit: VolumeUnit = VolumeUnit.ML,
    val suggestions: List<ProductCard> = emptyList(),
    val searchField: String? = null
)

@HiltViewModel
class ComparisonViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productRepo: ProductRepository,
    private val priceRepo: PriceEntryRepository
) : ViewModel() {
    private val _items = MutableStateFlow(listOf(CompInput(), CompInput()))
    val items: StateFlow<List<CompInput>> = _items.asStateFlow()

    private val searchJobs = mutableMapOf<Int, Job>()

    private val _results = MutableStateFlow<List<ComparisonResult>>(emptyList())
    val results: StateFlow<List<ComparisonResult>> = _results.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun update(index: Int, transform: (CompInput) -> CompInput) {
        _items.value = _items.value.toMutableList().also { it[index] = transform(it[index]) }
    }

    fun onFieldChange(index: Int, field: String, value: String) {
        val current = _items.value[index]
        val updated = when (field) {
            "name" -> current.copy(name = value, searchField = "name")
            "brand" -> current.copy(brand = value, searchField = "brand")
            "spec" -> current.copy(spec = value, searchField = "spec")
            else -> current
        }
        _items.value = _items.value.toMutableList().also { it[index] = updated }
        searchJobs[index]?.cancel()
        searchJobs[index] = viewModelScope.launch {
            delay(300)
            if (value.isNotBlank()) {
                val list = ProductSearch.search(productRepo, field, value)
                val cur = _items.value[index]
                _items.value = _items.value.toMutableList().also { it[index] = cur.copy(suggestions = list) }
                if (cur.name.isNotBlank() && cur.brand.isNotBlank() && cur.spec.isNotBlank() && cur.price.isBlank() && cur.qty.isBlank()) {
                    prefillPrice(index)
                }
            } else {
                val cur = _items.value[index]
                _items.value = _items.value.toMutableList().also { it[index] = cur.copy(suggestions = emptyList(), searchField = null) }
            }
        }
    }

    fun selectProduct(index: Int, card: ProductCard, field: String?) {
        val cur = _items.value[index]
        val (n, b, s) = ProductSearch.applySelection(card, field, cur.name, cur.brand, cur.spec)
        _items.value = _items.value.toMutableList().also {
            it[index] = cur.copy(name = n, brand = b, spec = s, unitType = card.unitType, suggestions = emptyList(), searchField = null)
        }
        prefillPrice(index)
    }

    private fun prefillPrice(index: Int) {
        val ci = _items.value[index]
        if (ci.name.isBlank() || ci.brand.isBlank() || ci.spec.isBlank()) return
        if (ci.price.isNotBlank() || ci.qty.isNotBlank()) return
        viewModelScope.launch {
            val product = productRepo.findOrCreateProduct(ci.name, ci.unitType)
            val variant = productRepo.findOrCreateVariant(product.id, ci.brand, ci.spec)
            val last = priceRepo.getLastByVariant(variant.id)
            if (last != null) {
                val fresh = _items.value[index]
                if (fresh.price.isBlank() && fresh.qty.isBlank()) {
                    _items.value = _items.value.toMutableList().also {
                        it[index] = fresh.copy(price = last.price, qty = last.quantity)
                    }
                }
            }
        }
    }

    fun replaceItems(newItems: List<CompInput>) {
        _items.value = newItems.take(4)
        _results.value = emptyList()
        _error.value = null
    }

    fun addItem() {
        if (_items.value.size < 4) _items.value = _items.value + CompInput()
    }

    fun removeItem(index: Int) {
        if (_items.value.size > 2) _items.value = _items.value.toMutableList().also { it.removeAt(index) }
    }

    fun calculate() {
        try {
            val compItems = _items.value.map { ci ->
                if (ci.price.isBlank() || ci.qty.isBlank()) {
                    throw IllegalArgumentException(context.getString(
                        R.string.error_need_price_qty,
                        ci.name.ifBlank { context.getString(R.string.error_generic_product) }
                    ))
                }
                ComparisonItem(ci.name.ifBlank { context.getString(R.string.field_product) }, PackageInput(
                    price = BigDecimal(ci.price), quantity = BigDecimal(ci.qty),
                    unitType = ci.unitType, massUnit = ci.massUnit, volumeUnit = ci.volumeUnit
                ), brand = ci.brand, spec = ci.spec)
            }
            _results.value = ComparisonCalculator.compare(compItems)
            _error.value = null
            viewModelScope.launch {
                for (ci in _items.value) {
                    if (ci.name.isNotBlank() && ci.price.isNotBlank() && ci.qty.isNotBlank()) {
                        val product = productRepo.findOrCreateProduct(ci.name, ci.unitType)
                        val variant = productRepo.findOrCreateVariant(product.id, ci.brand.ifBlank { null }, ci.spec.ifBlank { null })
                        val since = System.currentTimeMillis() - 5 * 60 * 1000
                        val existing = priceRepo.findRecentDuplicate(variant.id, ci.price, ci.qty, ci.unitType.name, since)
                        if (existing == null) {
                            priceRepo.insert(PriceEntryEntity(
                                variantId = variant.id,
                                price = ci.price,
                                quantity = ci.qty,
                                unit = ci.unitType.name,
                                currency = Currency.RUB,
                                source = "comparison-screen"
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _results.value = emptyList()
            _error.value = e.message ?: context.getString(R.string.error_generic)
        }
    }
}
