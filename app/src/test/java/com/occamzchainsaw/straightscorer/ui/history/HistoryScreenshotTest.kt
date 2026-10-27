package com.occamzchainsaw.straightscorer.ui.history

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import com.occamzchainsaw.straightscorer.viewmodels.HistoryUiState
import com.occamzchainsaw.straightscorer.viewmodels.MatchDetailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
class HistoryScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { StraightScorerTheme(darkTheme = dark) { content() } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("screenshots/history/$name.png")
    }

    @Test
    fun list_light() = capture("list_light") {
        HistoryContent(HistoryUiState(matches = SampleMatches.list(), isLoading = false), HistoryActions())
    }

    @Test
    fun list_dark() = capture("list_dark", dark = true) {
        HistoryContent(HistoryUiState(matches = SampleMatches.list(), isLoading = false), HistoryActions())
    }

    @Test
    fun empty() = capture("empty") {
        HistoryContent(HistoryUiState(isLoading = false), HistoryActions())
    }

    @Test
    fun detail_headToHead() = capture("detail_head_to_head") {
        MatchDetailContent(MatchDetailUiState.Loaded(SampleMatches.headToHead()), onBack = {}, onDelete = {})
    }

    @Test
    fun detail_practice_dark() = capture("detail_practice_dark", dark = true) {
        MatchDetailContent(MatchDetailUiState.Loaded(SampleMatches.practice()), onBack = {}, onDelete = {})
    }
}
