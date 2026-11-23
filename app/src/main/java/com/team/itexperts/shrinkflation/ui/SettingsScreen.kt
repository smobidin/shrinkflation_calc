package com.team.itexperts.shrinkflation.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.team.itexperts.shrinkflation.R
import com.team.itexperts.shrinkflation.model.AppLanguage
import com.team.itexperts.shrinkflation.model.Currency
import com.team.itexperts.shrinkflation.model.MassUnit
import com.team.itexperts.shrinkflation.model.ThemeMode
import com.team.itexperts.shrinkflation.model.VolumeUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LabeledDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val currency by vm.currency.collectAsState()
    val massUnit by vm.massUnit.collectAsState()
    val volumeUnit by vm.volumeUnit.collectAsState()
    val language by vm.language.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val savePrices by vm.savePrices.collectAsState()
    val storageLimit by vm.storageLimit.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        LabeledDropdown(
            label = stringResource(R.string.settings_language),
            selected = language,
            options = AppLanguage.values().toList(),
            optionLabel = {
                when (it) {
                    AppLanguage.RU -> stringResource(R.string.lang_ru)
                    AppLanguage.EN -> stringResource(R.string.lang_en)
                }
            },
            onSelect = {
                scope.launch {
                    vm.setLanguage(it)
                    (context as? Activity)?.recreate()
                }
            }
        )

        Spacer(Modifier.height(8.dp))
        LabeledDropdown(
            label = stringResource(R.string.settings_theme),
            selected = themeMode,
            options = ThemeMode.values().toList(),
            optionLabel = {
                when (it) {
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                }
            },
            onSelect = { vm.setThemeMode(it) }
        )

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_currency))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Currency.values().forEach { c ->
                FilterChip(selected = currency == c, onClick = { vm.setCurrency(c) }, label = { Text(c.name) })
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_mass_unit))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MassUnit.values().forEach { u ->
                FilterChip(selected = massUnit == u, onClick = { vm.setMassUnit(u) }, label = { Text(massUnitLabel(u)) })
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_volume_unit))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VolumeUnit.values().forEach { u ->
                FilterChip(selected = volumeUnit == u, onClick = { vm.setVolumeUnit(u) }, label = { Text(volumeUnitLabel(u)) })
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = savePrices, onCheckedChange = { vm.setSavePrices(it) })
            Text(stringResource(R.string.settings_save_prices))
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_storage_limit))
        OutlinedTextField(value = storageLimit.toString(), onValueChange = { v ->
            v.toIntOrNull()?.let { vm.setStorageLimit(it) }
        }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))
        Button(onClick = { scope.launch { vm.resetAll() } }) { Text(stringResource(R.string.settings_reset_all)) }
    }
}
