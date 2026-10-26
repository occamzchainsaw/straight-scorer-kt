package com.occamzchainsaw.straightscorer.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.core.models.MatchSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DEFAULT_TARGET_SCORE = intPreferencesKey("default_target_score")
        val RESET_RACK_ON_THIRD_FOUL = booleanPreferencesKey("reset_rack_on_third_foul")
        val COUNT_MISSES_IN_AVERAGE = booleanPreferencesKey("count_misses_in_average")
        val COUNT_SAFETIES_IN_AVERAGE = booleanPreferencesKey("count_safeties_in_average")
    }

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) = set(Keys.THEME_MODE, mode.name)
    override suspend fun setDynamicColor(enabled: Boolean) = set(Keys.DYNAMIC_COLOR, enabled)
    override suspend fun setKeepScreenOn(enabled: Boolean) = set(Keys.KEEP_SCREEN_ON, enabled)

    override suspend fun setDefaultTargetScore(score: Int) = set(
        Keys.DEFAULT_TARGET_SCORE,
        score.coerceIn(MatchSetup.MIN_TARGET_SCORE, MatchSetup.MAX_TARGET_SCORE)
    )

    override suspend fun setResetRackOnThirdFoul(enabled: Boolean) = set(Keys.RESET_RACK_ON_THIRD_FOUL, enabled)
    override suspend fun setCountMissesInAverage(enabled: Boolean) = set(Keys.COUNT_MISSES_IN_AVERAGE, enabled)
    override suspend fun setCountSafetiesInAverage(enabled: Boolean) = set(Keys.COUNT_SAFETIES_IN_AVERAGE, enabled)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        val gameDefaults = defaults.game
        return AppSettings(
            themeMode = this[Keys.THEME_MODE]
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: defaults.themeMode,
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
            defaultTargetScore = this[Keys.DEFAULT_TARGET_SCORE] ?: defaults.defaultTargetScore,
            game = GameSettings(
                resetRackOnThirdFoul = this[Keys.RESET_RACK_ON_THIRD_FOUL] ?: gameDefaults.resetRackOnThirdFoul,
                countMissesInAverage = this[Keys.COUNT_MISSES_IN_AVERAGE] ?: gameDefaults.countMissesInAverage,
                countSafetiesInAverage = this[Keys.COUNT_SAFETIES_IN_AVERAGE] ?: gameDefaults.countSafetiesInAverage
            )
        )
    }
}
