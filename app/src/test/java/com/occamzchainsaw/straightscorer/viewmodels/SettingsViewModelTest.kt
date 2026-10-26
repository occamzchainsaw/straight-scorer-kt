package com.occamzchainsaw.straightscorer.viewmodels

import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.data.settings.ThemeMode
import com.occamzchainsaw.straightscorer.testing.FakeSettingsRepository
import com.occamzchainsaw.straightscorer.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()
    private val viewModel by lazy { SettingsViewModel(repository) }

    @Test
    fun setters_updateRepository_andState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }

        viewModel.setThemeMode(ThemeMode.Light)
        viewModel.setDynamicColor(false)
        viewModel.setKeepScreenOn(false)
        viewModel.setDefaultTargetScore(125)
        viewModel.setResetRackOnThirdFoul(false)
        viewModel.setCountMissesInAverage(false)
        viewModel.setCountSafetiesInAverage(true)

        val expected = AppSettings(
            themeMode = ThemeMode.Light,
            dynamicColor = false,
            keepScreenOn = false,
            defaultTargetScore = 125,
            game = GameSettings(
                resetRackOnThirdFoul = false,
                countMissesInAverage = false,
                countSafetiesInAverage = true
            )
        )
        assertEquals(expected, repository.settings.value)
        assertEquals(expected, viewModel.settings.value)
    }
}
