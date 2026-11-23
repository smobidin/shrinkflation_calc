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
fun ShrinkflationScreen(vm: ShrinkflationViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.title_shrinkflation), style = MaterialTheme.typography.headlineSmall)
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
            Checkbox(checked = s.saveProduct, onCheckedChange = { vm.update { it.copy(saveProduct = it.saveProduct.not()) } })
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
        Text(stringResource(R.string.label_old_package))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = s.priceOld, onValueChange = { v -> vm.update { it.copy(priceOld = v) } },
                label = { Text(stringResource(R.string.field_price)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("priceOld"))
            OutlinedTextField(value = s.qtyOld, onValueChange = { v -> vm.update { it.copy(qtyOld = v) } },
                label = { Text(stringResource(R.string.field_qty)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("qtyOld"))
        }
        if (s.unitType == UnitType.MASS) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MassUnit.values().forEach { u ->
                    FilterChip(selected = s.massUnitOld == u, onClick = { vm.update { it.copy(massUnitOld = u) } }, label = { Text(massUnitLabel(u)) })
                }
            }
        } else if (s.unitType == UnitType.VOLUME) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VolumeUnit.values().forEach { u ->
                    FilterChip(selected = s.volumeUnitOld == u, onClick = { vm.update { it.copy(volumeUnitOld = u) } }, label = { Text(volumeUnitLabel(u)) })
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.label_new_package))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = s.priceNew, onValueChange = { v -> vm.update { it.copy(priceNew = v) } },
                label = { Text(stringResource(R.string.field_price)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("priceNew"))
            OutlinedTextField(value = s.qtyNew, onValueChange = { v -> vm.update { it.copy(qtyNew = v) } },
                label = { Text(stringResource(R.string.field_qty)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("qtyNew"))
        }
        if (s.unitType == UnitType.MASS) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MassUnit.values().forEach { u ->
                    FilterChip(selected = s.massUnitNew == u, onClick = { vm.update { it.copy(massUnitNew = u) } }, label = { Text(massUnitLabel(u)) })
                }
            }
        } else if (s.unitType == UnitType.VOLUME) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VolumeUnit.values().forEach { u ->
                    FilterChip(selected = s.volumeUnitNew == u, onClick = { vm.update { it.copy(volumeUnitNew = u) } }, label = { Text(volumeUnitLabel(u)) })
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.calculate() }, modifier = Modifier.testTag("calculateBtn")) { Text(stringResource(R.string.action_calculate)) }
            OutlinedButton(onClick = { vm.swap() }) { Text(stringResource(R.string.action_swap)) }
            OutlinedButton(onClick = { vm.reset() }) { Text(stringResource(R.string.action_reset)) }
        }

        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        s.result?.let { r ->
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.result_unit_old, r.unitPriceOld.toString()))
                    Text(stringResource(R.string.result_unit_new, r.unitPriceNew.toString()))
                    Text(stringResource(R.string.result_change, r.changePercent.toString()))
                    Text(stringResource(R.string.result_equiv, r.equivalentOldPrice.toString()))
                    Text(stringResource(R.string.result_overpay, r.overpay.toString()))
                    Text(stringResource(R.string.result_target, r.targetPrice.toString()))
                    Text(stringResource(R.string.result_discount, r.discountNeededPercent.toString()))
                }
            }
        }
    }
}
