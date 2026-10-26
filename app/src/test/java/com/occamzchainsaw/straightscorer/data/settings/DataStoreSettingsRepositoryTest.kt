package com.occamzchainsaw.straightscorer.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.occamzchainsaw.core.models.GameSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun settingsTest(block: suspend TestScope.(DataStoreSettingsRepository) -> Unit) =
        runTest(UnconfinedTestDispatcher()) {
            val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
                tempFolder.newFile("settings.preferences_pb").also { it.delete() }
            }
            block(DataStoreSettingsRepository(dataStore))
        }

    @Test
    fun emptyStore_returnsDefaults() = settingsTest { repository ->
        val settings = repository.settings.first()

        assertEquals(AppSettings(), settings)
        assertEquals(true, settings.game.countMissesInAverage)
        assertEquals(false, settings.game.countSafetiesInAverage)
        assertEquals(true, settings.game.resetRackOnThirdFoul)
    }

    @Test
    fun setters_arePersisted() = settingsTest { repository ->
        repository.setThemeMode(ThemeMode.Dark)
        repository.setDynamicColor(false)
        repository.setKeepScreenOn(false)
        repository.setDefaultTargetScore(150)
        repository.setResetRackOnThirdFoul(false)
        repository.setCountMissesInAverage(false)
        repository.setCountSafetiesInAverage(true)

        assertEquals(
            AppSettings(
                themeMode = ThemeMode.Dark,
                dynamicColor = false,
                keepScreenOn = false,
                defaultTargetScore = 150,
                game = GameSettings(
                    resetRackOnThirdFoul = false,
                    countMissesInAverage = false,
                    countSafetiesInAverage = true
                )
            ),
            repository.settings.first()
        )
    }

    @Test
    fun defaultTargetScore_isClampedToValidRange() = settingsTest { repository ->
        repository.setDefaultTargetScore(3)
        assertEquals(10, repository.settings.first().defaultTargetScore)

        repository.setDefaultTargetScore(5000)
        assertEquals(1000, repository.settings.first().defaultTargetScore)
    }
}
