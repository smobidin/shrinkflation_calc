package com.team.itexperts.shrinkflation.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.data.CatalogRow
import com.team.itexperts.shrinkflation.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    vm: CatalogViewModel = hiltViewModel(),
    selectionVm: ComparisonSelectionViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    onNavigateToComparison: () -> Unit,
    onNavigateToCalculate: () -> Unit
) {
    val state by vm.state.collectAsState()
    var deleteTarget by remember { mutableStateOf<CatalogRow?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.catalog_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        var typeExpanded by remember { mutableStateOf(false) }
        val typeLabel = when (state.typeFilter) {
            null -> stringResource(R.string.catalog_filter_all)
            UnitType.MASS -> stringResource(R.string.unit_mass)
            UnitType.VOLUME -> stringResource(R.string.unit_volume)
            UnitType.COUNT -> stringResource(R.string.unit_count)
        }
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
            OutlinedTextField(
                value = typeLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.catalog_filter_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                (listOf(null) + UnitType.values().toList()).forEach { type ->
                    val label = when (type) {
                        null -> stringResource(R.string.catalog_filter_all)
                        UnitType.MASS -> stringResource(R.string.unit_mass)
                        UnitType.VOLUME -> stringResource(R.string.unit_volume)
                        UnitType.COUNT -> stringResource(R.string.unit_count)
                    }
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { vm.setTypeFilter(type); typeExpanded = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        SuggestField(
            label = stringResource(R.string.catalog_filter_brand),
            value = state.brandFilter,
            suggestions = state.brandSuggestions,
            onChange = vm::setBrandFilter,
            onSelect = vm::selectBrandSuggestion
        )
        Spacer(Modifier.height(8.dp))
        SuggestField(
            label = stringResource(R.string.catalog_filter_spec),
            value = state.specFilter,
            suggestions = state.specSuggestions,
            onChange = vm::setSpecFilter,
            onSelect = vm::selectSpecSuggestion
        )
        Spacer(Modifier.height(12.dp))

        if (state.selected.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.catalog_selected, state.selected.size))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::clearSelection) {
                    Text(stringResource(R.string.catalog_clear_selection))
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                state.selectedRows.forEach { row ->
                    FilterChip(
                        selected = true,
                        onClick = { vm.toggle(row.key) },
                        label = { Text(comparisonItemLabel(row.name, row.brand ?: "", row.spec ?: "")) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (state.all.isEmpty()) {
            Text(stringResource(R.string.catalog_empty), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onNavigateToCalculate) {
                Text(stringResource(R.string.catalog_go_calculate))
            }
        } else {
            state.visible.forEach { row ->
                CatalogRowItem(
                    row = row,
                    selected = row.key in state.selected,
                    selectionFull = state.selected.size >= 4 && row.key !in state.selected,
                    onToggle = { vm.toggle(row.key) },
                    onEdit = { vm.startEdit(row) },
                    onDelete = { deleteTarget = row }
                )
            }
            if (state.visible.isEmpty()) {
                Text(stringResource(R.string.catalog_empty), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                vm.compare(selectionVm)
                onNavigateToComparison()
            },
            enabled = state.selected.size in 2..4,
            modifier = Modifier.fillMaxWidth().testTag("catalog_compare")
        ) {
            Text(stringResource(R.string.catalog_compare, state.selected.size))
        }
        if (state.selected.size !in 2..4) {
            Text(stringResource(R.string.catalog_select_hint), style = MaterialTheme.typography.bodySmall)
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    state.editing?.let { edit ->
        EditDialog(
            edit = edit,
            onChange = { changed -> vm.updateEdit { changed } },
            onSave = vm::saveEdit,
            onCancel = vm::cancelEdit
        )
    }

    deleteTarget?.let { row ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.catalog_delete_title)) },
            text = { Text(stringResource(R.string.catalog_delete_message, row.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(row)
                    deleteTarget = null
                }) { Text(stringResource(R.string.catalog_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.catalog_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun SuggestField(
    label: String,
    value: String,
    suggestions: List<String>,
    onChange: (String) -> Unit,
    onSelect: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        if (suggestions.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    suggestions.forEach { suggestion ->
                        TextButton(
                            onClick = { onSelect(suggestion) },
                            modifier = Modifier.fillMaxWidth().testTag("filter_suggestion")
                        ) { Text(suggestion) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogRowItem(
    row: CatalogRow,
    selected: Boolean,
    selectionFull: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete, modifier = Modifier.testTag("catalog_delete")) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.catalog_delete))
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = !selectionFull,
                modifier = Modifier.testTag("catalog_checkbox_${row.name}")
            )
            Column(Modifier.weight(1f)) {
                Text(
                    comparisonItemLabel(row.name, row.brand ?: "", row.spec ?: ""),
                    style = MaterialTheme.typography.titleSmall
                )
                val priceText = if (row.price != null && row.quantity != null) {
                    stringResource(
                        R.string.catalog_row_price,
                        row.price,
                        row.quantity,
                        rowUnitLabel(row)
                    )
                } else {
                    stringResource(R.string.catalog_no_price)
                }
                Text(priceText, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit, modifier = Modifier.testTag("catalog_edit")) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.catalog_edit))
            }
        }
    }
}

@Composable
private fun rowUnitLabel(row: CatalogRow): String = when (row.unit) {
    MassUnit.KG.name -> stringResource(R.string.unit_kg)
    VolumeUnit.L.name -> stringResource(R.string.unit_l)
    VolumeUnit.ML.name -> stringResource(R.string.unit_ml)
    UnitType.COUNT.name -> stringResource(R.string.unit_pcs)
    else -> when (row.unitType) {
        UnitType.MASS -> stringResource(R.string.unit_g)
        UnitType.VOLUME -> stringResource(R.string.unit_ml)
        UnitType.COUNT -> stringResource(R.string.unit_pcs)
    }
}

@Composable
private fun EditDialog(
    edit: CatalogEdit,
    onChange: (CatalogEdit) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.catalog_dialog_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = edit.name,
                    onValueChange = { onChange(edit.copy(name = it)) },
                    label = { Text(stringResource(R.string.field_product)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = edit.brand,
                        onValueChange = { onChange(edit.copy(brand = it)) },
                        label = { Text(stringResource(R.string.field_brand)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = edit.spec,
                        onValueChange = { onChange(edit.copy(spec = it)) },
                        label = { Text(stringResource(R.string.field_spec)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UnitType.values().forEach { type ->
                        FilterChip(
                            selected = edit.unitType == type,
                            onClick = { onChange(edit.copy(unitType = type)) },
                            label = { Text(unitTypeLabel(type)) }
                        )
                    }
                }
                when (edit.unitType) {
                    UnitType.MASS -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MassUnit.values().forEach { unit ->
                            FilterChip(
                                selected = edit.massUnit == unit,
                                onClick = { onChange(edit.copy(massUnit = unit)) },
                                label = { Text(massUnitLabel(unit)) }
                            )
                        }
                    }
                    UnitType.VOLUME -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VolumeUnit.values().forEach { unit ->
                            FilterChip(
                                selected = edit.volumeUnit == unit,
                                onClick = { onChange(edit.copy(volumeUnit = unit)) },
                                label = { Text(volumeUnitLabel(unit)) }
                            )
                        }
                    }
                    UnitType.COUNT -> Unit
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = edit.price,
                        onValueChange = { onChange(edit.copy(price = it)) },
                        label = { Text(stringResource(R.string.field_price)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = edit.quantity,
                        onValueChange = { onChange(edit.copy(quantity = it)) },
                        label = { Text(stringResource(R.string.field_qty)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                edit.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.catalog_dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.catalog_dialog_cancel)) }
        }
    )
}
