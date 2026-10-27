package com.occamzchainsaw.straightscorer.ui.settings

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.data.settings.ThemeMode
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SettingsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun render(settings: AppSettings = AppSettings(), dark: Boolean = false) {
        composeRule.setContent {
            StraightScorerTheme(darkTheme = dark) {
                SettingsContent(settings, SettingsActions(), versionName = "1.0")
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun defaults_light() {
        render()
        composeRule.onRoot().captureRoboImage("screenshots/settings/defaults_light.png")
    }

    @Test
    fun changed_dark() {
        render(
            AppSettings(
                themeMode = ThemeMode.Dark,
                keepScreenOn = false,
                defaultTargetScore = 150,
                game = GameSettings(countSafetiesInAverage = true)
            ),
            dark = true
        )
        composeRule.onRoot().captureRoboImage("screenshots/settings/changed_dark.png")
    }

    @Test
    fun licenses() {
        composeRule.setContent { StraightScorerTheme { LicensesScreen(onBack = {}) } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("screenshots/settings/licenses.png")
    }
}
