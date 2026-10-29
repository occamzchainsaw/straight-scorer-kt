package com.occamzchainsaw.straightscorer.ui.game

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import com.occamzchainsaw.straightscorer.viewmodels.GameUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The scoreboards with one card turned over to its stats side. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScoreboardFlipScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun renderFlipped(state: GameUiState, flip: String, name: String, dark: Boolean = false) {
        composeRule.setContent {
            StraightScorerTheme(darkTheme = dark) {
                GameContent(state = state, actions = GameActions(), showAddPointsSheet = false)
            }
        }
        // names also appear in the history below; the scoreboard card comes first
        composeRule.onAllNodesWithText(flip).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("screenshots/game/$name.png")
    }

    @Test
    fun multiPlayer_flipped() = renderFlipped(SampleGames.multiPlayer(), flip = "Carol", name = "multi_player_flipped")

    @Test
    fun multiPlayer_flipped_dark() =
        renderFlipped(SampleGames.multiPlayer(), flip = "Dave", name = "multi_player_flipped_dark", dark = true)
}
