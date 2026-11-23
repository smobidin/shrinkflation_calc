package com.team.itexperts.shrinkflation.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.team.itexperts.shrinkflation.data.PriceEntryRepository
import com.team.itexperts.shrinkflation.data.ProductRepository
import com.team.itexperts.shrinkflation.model.AppLanguage
import com.team.itexperts.shrinkflation.model.Currency
import com.team.itexperts.shrinkflation.model.MassUnit
import com.team.itexperts.shrinkflation.model.ThemeMode
import com.team.itexperts.shrinkflation.model.VolumeUnit
import com.team.itexperts.shrinkflation.storage.Keys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val productRepo: ProductRepository,
    private val priceRepo: PriceEntryRepository
) : ViewModel() {
    val currency: StateFlow<Currency> = dataStore.data.map { Currency.valueOf(it[Keys.CURRENCY] ?: Currency.RUB.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Currency.RUB)
    val massUnit: StateFlow<MassUnit> = dataStore.data.map { MassUnit.valueOf(it[Keys.MASS_UNIT] ?: MassUnit.G.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MassUnit.G)
    val volumeUnit: StateFlow<VolumeUnit> = dataStore.data.map { VolumeUnit.valueOf(it[Keys.VOLUME_UNIT] ?: VolumeUnit.ML.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VolumeUnit.ML)
    val language: StateFlow<AppLanguage> = dataStore.data.map { prefs ->
        val code = prefs[Keys.LANGUAGE] ?: AppLanguage.RU.code
        AppLanguage.values().firstOrNull { it.code == code } ?: AppLanguage.RU
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.RU)
    val themeMode: StateFlow<ThemeMode> = dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val savePrices: StateFlow<Boolean> = dataStore.data.map { it[Keys.SAVE_PRICES] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val storageLimit: StateFlow<Int> = dataStore.data.map { it[Keys.STORAGE_LIMIT] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setCurrency(c: Currency) = viewModelScope.launch { dataStore.edit { it[Keys.CURRENCY] = c.name } }
    fun setMassUnit(u: MassUnit) = viewModelScope.launch { dataStore.edit { it[Keys.MASS_UNIT] = u.name } }
    fun setVolumeUnit(u: VolumeUnit) = viewModelScope.launch { dataStore.edit { it[Keys.VOLUME_UNIT] = u.name } }
    suspend fun setLanguage(l: AppLanguage) { dataStore.edit { it[Keys.LANGUAGE] = l.code } }
    fun setThemeMode(m: ThemeMode) = viewModelScope.launch { dataStore.edit { it[Keys.THEME] = m.name } }
    fun setSavePrices(v: Boolean) = viewModelScope.launch { dataStore.edit { it[Keys.SAVE_PRICES] = v } }
    fun setStorageLimit(v: Int) = viewModelScope.launch { dataStore.edit { it[Keys.STORAGE_LIMIT] = v } }

    fun resetAll() = viewModelScope.launch {
        dataStore.edit { it.clear() }
        priceRepo.deleteAll()
        // products? keep? delete all too
    }
}
