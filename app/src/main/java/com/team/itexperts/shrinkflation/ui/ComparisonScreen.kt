package com.team.itexperts.shrinkflation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.model.*
import java.text.NumberFormat

@Composable
fun ComparisonScreen(
    vm: ComparisonViewModel = hiltViewModel(),
    selectionVm: ComparisonSelectionViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    onNavigateToCatalog: () -> Unit
) {
    LaunchedEffect(Unit) {
        val pending = selectionVm.pending.value
        if (pending.isNotEmpty()) {
            vm.replaceItems(pending)
            selectionVm.consume()
        }
    }
    val items by vm.items.collectAsState()
    val results by vm.results.collectAsState()
    val error by vm.error.collectAsState()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val currency by settingsVm.currency.collectAsState()
    val numberFormat = remember {
        NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }
    val currencySymbol = when (currency) {
        Currency.RUB -> "₽"
        Currency.USD -> "$"
        Currency.EUR -> "€"
        Currency.GBP -> "£"
        Currency.CNY -> "¥"
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.title_comparison), style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = onNavigateToCatalog) {
            Text(stringResource(R.string.catalog_choose_for_compare))
        }
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { i, ci ->
            OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(8.dp)) {
                    ProductFields(
                        name = ci.name,
                        brand = ci.brand,
                        spec = ci.spec,
                        suggestions = ci.suggestions,
                        searchField = ci.searchField,
                        onNameChange = { v -> vm.onFieldChange(i, "name", v) },
                        onBrandChange = { v -> vm.onFieldChange(i, "brand", v) },
                        onSpecChange = { v -> vm.onFieldChange(i, "spec", v) },
                        onSelect = { card, field -> vm.selectProduct(i, card, field) }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ci.price, onValueChange = { v -> vm.update(i) { it.copy(price = v) } },
                            label = { Text(stringResource(R.string.field_price)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ci.qty, onValueChange = { v -> vm.update(i) { it.copy(qty = v) } },
                            label = { Text(stringResource(R.string.field_qty)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitType.values().forEach { u ->
                            FilterChip(selected = ci.unitType == u, onClick = { vm.update(i) { it.copy(unitType = u) } }, label = { Text(unitTypeLabel(u)) })
                        }
                    }
                    if (ci.unitType == UnitType.MASS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MassUnit.values().forEach { u ->
                                FilterChip(selected = ci.massUnit == u, onClick = { vm.update(i) { it.copy(massUnit = u) } }, label = { Text(massUnitLabel(u)) })
                            }
                        }
                    } else if (ci.unitType == UnitType.VOLUME) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VolumeUnit.values().forEach { u ->
                                FilterChip(selected = ci.volumeUnit == u, onClick = { vm.update(i) { it.copy(volumeUnit = u) } }, label = { Text(volumeUnitLabel(u)) })
                            }
                        }
                    }
                    if (items.size > 2) {
                        TextButton(onClick = { vm.removeItem(i) }) { Text(stringResource(R.string.action_remove)) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (items.size < 4) Button(onClick = { vm.addItem() }) { Text(stringResource(R.string.action_add)) }
            Button(onClick = { vm.calculate() }) { Text(stringResource(R.string.action_compare)) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (results.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.comparison_result_header), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.comparison_result_hint, wholeUnitLabelLocalized(results.first().unit)),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            results.forEach { r ->
                val label = comparisonItemLabel(r.name, r.brand, r.spec)
                val unitLabel = wholeUnitLabelLocalized(r.unit)
                val priceText = comparisonPriceText(numberFormat.format(r.wholeUnitPrice), currencySymbol, unitLabel)
                if (r.isCheapest) {
                    Text(stringResource(R.string.comparison_cheapest_line, label, priceText))
                } else {
                    val pct = numberFormat.format(r.diffPercent)
                    val absText = "+" + comparisonPriceText(numberFormat.format(r.absoluteDiff), currencySymbol, unitLabel)
                    Text(stringResource(R.string.comparison_more_line, label, priceText, pct, absText))
                }
            }
        }
    }
}
