package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.data.settings.SettingsRepository
import com.occamzchainsaw.straightscorer.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setThemeMode(mode: ThemeMode) = update { setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = update { setDynamicColor(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = update { setKeepScreenOn(enabled) }
    fun setDefaultTargetScore(score: Int) = update { setDefaultTargetScore(score) }
    fun setResetRackOnThirdFoul(enabled: Boolean) = update { setResetRackOnThirdFoul(enabled) }
    fun setCountMissesInAverage(enabled: Boolean) = update { setCountMissesInAverage(enabled) }
    fun setCountSafetiesInAverage(enabled: Boolean) = update { setCountSafetiesInAverage(enabled) }

    private fun update(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { repository.block() }
    }
}
