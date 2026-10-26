package com.occamzchainsaw.straightscorer.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setDefaultTargetScore(score: Int)
    suspend fun setResetRackOnThirdFoul(enabled: Boolean)
    suspend fun setCountMissesInAverage(enabled: Boolean)
    suspend fun setCountSafetiesInAverage(enabled: Boolean)
}
