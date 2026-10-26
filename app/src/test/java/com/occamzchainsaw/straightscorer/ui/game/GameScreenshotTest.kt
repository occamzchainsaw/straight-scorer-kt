package com.occamzchainsaw.straightscorer.ui.game

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import com.occamzchainsaw.straightscorer.viewmodels.GameUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the game screen to PNGs in app/screenshots/game.
 * Record with: ./gradlew :app:recordRoborazziDebug --tests "*ScreenshotTest"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class GameScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun render(state: GameUiState, dark: Boolean = false, showAddPoints: Boolean = false) {
        composeRule.setContent {
            StraightScorerTheme(darkTheme = dark) {
                GameContent(state = state, actions = GameActions(), showAddPointsSheet = showAddPoints)
            }
        }
        composeRule.waitForIdle()
    }

    private fun capture(name: String) = composeRule.onRoot().captureRoboImage("screenshots/game/$name.png")

    /** Includes dialogs and bottom sheets, which live in their own windows. */
    private fun captureScreen(name: String) = captureScreenRoboImage("screenshots/game/$name.png")

    @Test
    fun headToHead_light() {
        render(SampleGames.headToHead())
        capture("head_to_head_light")
    }

    @Test
    fun headToHead_dark() {
        render(SampleGames.headToHead(), dark = true)
        capture("head_to_head_dark")
    }

    @Test
    fun multiPlayer_light() {
        render(SampleGames.multiPlayer())
        capture("multi_player_light")
    }

    @Test
    fun multiPlayer_dark() {
        render(SampleGames.multiPlayer(), dark = true)
        capture("multi_player_dark")
    }

    @Test
    fun addPointsSheet() {
        render(SampleGames.headToHead(), showAddPoints = true)
        captureScreen("add_points_sheet")
    }

    @Test
    fun openingBreakFoulSheet() {
        render(SampleGames.openingBreakFoul())
        captureScreen("opening_break_foul_sheet")
    }

    @Test
    fun finishedGame() {
        render(SampleGames.finished())
        captureScreen("result_sheet")
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun headToHead_tablet() {
        render(SampleGames.headToHead())
        capture("head_to_head_tablet")
    }

    @Test
    fun openingBreak() {
        render(SampleGames.openingBreakFoul().copy(showOpeningBreakFoulDialog = false))
        capture("opening_break")
    }

    @Test
    fun practice() {
        render(SampleGames.practice())
        capture("practice")
    }

    @Test
    fun practiceFinished() {
        render(SampleGames.practiceFinished(), dark = true)
        captureScreen("practice_result_sheet")
    }
}
