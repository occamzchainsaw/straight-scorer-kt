package com.occamzchainsaw.straightscorer.ui.rules

import android.app.Application
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.occamzchainsaw.straightscorer.data.rules.DocBlock
import com.occamzchainsaw.straightscorer.data.rules.RulesDocument
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import com.occamzchainsaw.straightscorer.viewmodels.RulesSearchState
import com.occamzchainsaw.straightscorer.viewmodels.RulesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel7)
@OptIn(ExperimentalRoborazziApi::class)
class RulesScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val document = RulesDocument.parse(File("src/main/assets/rules.md").readText())

    private fun render(
        state: RulesUiState,
        dark: Boolean = false,
        firstItem: Int = 0,
        showContents: Boolean = false
    ) {
        composeRule.setContent {
            StraightScorerTheme(darkTheme = dark) {
                RulesContent(
                    state = state,
                    actions = RulesActions(),
                    listState = rememberLazyListState(firstItem),
                    initiallyShowContents = showContents
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun top_light() {
        render(RulesUiState.Loaded(document))
        composeRule.onRoot().captureRoboImage("screenshots/rules/top_light.png")
    }

    @Test
    fun chapter_dark() {
        val firstChapter = document.blocks.indexOfFirst { it is DocBlock.Heading && it.content.text.startsWith("4 ") }
        render(RulesUiState.Loaded(document), dark = true, firstItem = firstChapter)
        composeRule.onRoot().captureRoboImage("screenshots/rules/chapter_dark.png")
    }

    @Test
    fun table() {
        val table = document.blocks.indexOfFirst { it is DocBlock.Table }
        render(RulesUiState.Loaded(document), firstItem = table - 1)
        composeRule.onRoot().captureRoboImage("screenshots/rules/table.png")
    }

    @Test
    fun search() {
        val matches = document.search("head spot")
        render(RulesUiState.Loaded(document, RulesSearchState(isActive = true, query = "head spot", matches = matches, currentIndex = 1)))
        composeRule.onRoot().captureRoboImage("screenshots/rules/search.png")
    }

    @Test
    fun contents() {
        render(RulesUiState.Loaded(document), showContents = true)
        captureScreenRoboImage("screenshots/rules/contents_sheet.png")
    }
}
