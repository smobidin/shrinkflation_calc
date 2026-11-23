package com.team.itexperts.shrinkflation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.model.*

@Composable
fun UnitPriceScreen(vm: UnitPriceViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.title_unitprice), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        ProductFields(
            name = s.productName,
            brand = s.brand,
            spec = s.spec,
            suggestions = s.suggestions,
            searchField = s.searchField,
            onNameChange = { v -> vm.onProductNameChange(v) },
            onBrandChange = { v -> vm.onBrandChange(v) },
            onSpecChange = { v -> vm.onSpecChange(v) },
            onSelect = { card, field -> vm.selectProduct(card, field) }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = s.saveProduct, onCheckedChange = { vm.update { it.copy(saveProduct = !it.saveProduct) } })
            Text(stringResource(R.string.action_save_to_db))
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.label_type, unitTypeLabel(s.unitType)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitType.values().forEach { u ->
                FilterChip(selected = s.unitType == u, onClick = { vm.update { it.copy(unitType = u) } }, label = { Text(unitTypeLabel(u)) })
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = s.price, onValueChange = { v -> vm.update { it.copy(price = v) } },
                label = { Text(stringResource(R.string.field_price)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("price"))
            OutlinedTextField(value = s.qty, onValueChange = { v -> vm.update { it.copy(qty = v) } },
                label = { Text(stringResource(R.string.field_qty)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("qty"))
        }
        if (s.unitType == UnitType.MASS) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MassUnit.values().forEach { u ->
                    FilterChip(selected = s.massUnit == u, onClick = { vm.update { it.copy(massUnit = u) } }, label = { Text(massUnitLabel(u)) })
                }
            }
        } else if (s.unitType == UnitType.VOLUME) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VolumeUnit.values().forEach { u ->
                    FilterChip(selected = s.volumeUnit == u, onClick = { vm.update { it.copy(volumeUnit = u) } }, label = { Text(volumeUnitLabel(u)) })
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.calculate() }, modifier = Modifier.testTag("calculateBtn")) { Text(stringResource(R.string.action_calculate)) }
            OutlinedButton(onClick = { vm.reset() }) { Text(stringResource(R.string.action_reset)) }
        }

        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        s.result?.let { r ->
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.result_unit_price, wholeUnitLabelLocalized(s.unitType), r.toString()))
                }
            }
        }
    }
}
