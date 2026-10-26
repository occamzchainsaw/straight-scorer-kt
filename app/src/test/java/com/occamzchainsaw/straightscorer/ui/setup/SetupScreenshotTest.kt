package com.occamzchainsaw.straightscorer.ui.setup

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.occamzchainsaw.straightscorer.dtos.PlayerSetupItem
import com.occamzchainsaw.straightscorer.ui.game.SampleGames
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import com.occamzchainsaw.straightscorer.viewmodels.SetupUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SetupScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val twoPlayers = SetupUiState(
        players = listOf(PlayerSetupItem(name = "Alice", isStarting = true), PlayerSetupItem(name = "Bob")),
        targetScoreText = "100"
    )

    private fun render(state: SetupUiState, dark: Boolean = false, name: String) {
        composeRule.setContent { StraightScorerTheme(darkTheme = dark) { SetupContent(state, SetupActions()) } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("screenshots/setup/$name.png")
    }

    @Test
    fun twoPlayers_light() = render(twoPlayers, name = "two_players_light")

    @Test
    fun withGameInProgress_dark() = render(
        twoPlayers.copy(activeGame = SampleGames.headToHead().game),
        dark = true,
        name = "resume_dark"
    )

    @Test
    fun practice() = render(
        twoPlayers.copy(players = listOf(PlayerSetupItem(name = "Alice", isStarting = true))),
        name = "practice"
    )

    @Test
    fun errors() = render(
        twoPlayers.copy(
            players = listOf(
                PlayerSetupItem(name = "", isStarting = true),
                PlayerSetupItem(name = "Bob", headStart = "120"),
                PlayerSetupItem(name = "bob")
            ),
            targetScoreText = "100",
            showErrors = true
        ),
        name = "errors"
    )
}
