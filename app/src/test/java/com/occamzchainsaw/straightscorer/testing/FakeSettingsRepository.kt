package com.occamzchainsaw.straightscorer.testing

import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.data.settings.SettingsRepository
import com.occamzchainsaw.straightscorer.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun setThemeMode(mode: ThemeMode) = settings.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = settings.update { it.copy(dynamicColor = enabled) }
    override suspend fun setKeepScreenOn(enabled: Boolean) = settings.update { it.copy(keepScreenOn = enabled) }
    override suspend fun setDefaultTargetScore(score: Int) = settings.update { it.copy(defaultTargetScore = score) }
    override suspend fun setResetRackOnThirdFoul(enabled: Boolean) =
        settings.update { it.copy(game = it.game.copy(resetRackOnThirdFoul = enabled)) }
    override suspend fun setCountMissesInAverage(enabled: Boolean) =
        settings.update { it.copy(game = it.game.copy(countMissesInAverage = enabled)) }
    override suspend fun setCountSafetiesInAverage(enabled: Boolean) =
        settings.update { it.copy(game = it.game.copy(countSafetiesInAverage = enabled)) }
}
