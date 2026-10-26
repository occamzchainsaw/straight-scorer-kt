package com.occamzchainsaw.straightscorer.viewmodels

import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.repositories.FakeMatchHistoryRepository
import com.occamzchainsaw.core.rules.toMatchResult
import com.occamzchainsaw.core.services.GameEngine
import com.occamzchainsaw.core.services.MatchRecorder
import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.testing.FakeSettingsRepository
import com.occamzchainsaw.straightscorer.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val engine = GameEngine()
    private val history = FakeMatchHistoryRepository()
    private val recorder = MatchRecorder(engine, history)
    private val settings = FakeSettingsRepository()
    private val viewModel by lazy { GameViewModel(engine, recorder, settings) }

    private val state get() = viewModel.uiState.value!!

    private fun startGame(targetScore: Int = 20) = engine.startGame(
        MatchSetup(listOf(PlayerSetup("Alice"), PlayerSetup("Bob")), targetScore)
    )

    private fun TestScope.collectEvents(): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    @Test
    fun noGame_meansNoState() {
        assertNull(viewModel.uiState.value)
    }

    @Test
    fun state_followsEngine() {
        startGame()
        viewModel.addPoint()
        viewModel.miss()

        assertEquals(1, state.players[0].score)
        assertEquals(1, state.atTableIndex)
        assertTrue(state.canUndo)
        assertTrue(state.isHeadToHead)
        assertEquals(20, state.pointsToWin)
    }

    @Test
    fun keepScreenOn_followsSettings() {
        startGame()
        assertTrue(state.keepScreenOn)

        settings.settings.value = AppSettings(keepScreenOn = false)
        assertFalse(state.keepScreenOn)
    }

    @Test
    fun addPoints_isLimitedToPointsNeeded() {
        startGame(targetScore = 20)

        assertFalse(viewModel.addPoints(0))
        assertFalse(viewModel.addPoints(21))
        assertTrue(viewModel.addPoints(20))
        assertTrue(state.isFinished)
        assertFalse(viewModel.addPoints(1))
    }

    @Test
    fun foulOnOpeningBreak_asksWhatHappened() {
        startGame()
        viewModel.foul()

        assertTrue(state.showOpeningBreakFoulDialog)
        assertTrue(state.breaks.isEmpty())

        viewModel.dismissOpeningBreakFoulDialog()
        assertFalse(state.showOpeningBreakFoulDialog)
    }

    @Test
    fun openingBreakFoul_choices() {
        startGame()
        viewModel.foul()
        viewModel.resolveOpeningBreakFoul(OpeningBreakFoul.BreakingReBreak)
        assertEquals(-2, state.players[0].score)
        assertEquals(0, state.atTableIndex)
        assertFalse(state.showOpeningBreakFoulDialog)

        viewModel.foul()
        viewModel.resolveOpeningBreakFoul(OpeningBreakFoul.BreakingAcceptTable)
        assertEquals(-4, state.players[0].score)
        assertEquals(1, state.atTableIndex)
        assertEquals(BreakEndAction.BreakingFoul, state.breaks.last().endAction)
    }

    @Test
    fun standardFoulOnBreak_countsTowardsConsecutiveFouls() {
        startGame()
        viewModel.foul()
        viewModel.resolveOpeningBreakFoul(OpeningBreakFoul.Standard)

        assertEquals(-1, state.players[0].score)
        assertEquals(1, state.players[0].consecutiveFouls)
    }

    @Test
    fun foulAfterBreak_isStandardWithoutDialog() {
        startGame()
        viewModel.addPoint()
        viewModel.foul()

        assertFalse(state.showOpeningBreakFoulDialog)
        assertEquals(0, state.players[0].score)
        assertEquals(1, state.atTableIndex)
    }

    @Test
    fun closeFinishedGame_savesResult_andNavigatesHome() = runTest {
        val events = collectEvents()
        startGame(targetScore = 10)
        viewModel.addPoints(10)
        viewModel.closeFinishedGame()

        assertEquals("Alice", history.matches.value.values.single().winner?.name)
        assertNull(viewModel.uiState.value)
        assertEquals(listOf(GameEvent.NavigateHome), events)
    }

    @Test
    fun closeFinishedGame_ignoredWhileInProgress() = runTest {
        val events = collectEvents()
        startGame()
        viewModel.closeFinishedGame()

        assertTrue(events.isEmpty())
        assertFalse(state.isFinished)
    }

    @Test
    fun discardResult_removesOnlyThisGamesResult() = runTest {
        val events = collectEvents()
        startGame(targetScore = 10)
        viewModel.addPoints(10)
        val earlierResult = engine.state.value!!.toMatchResult()!!
        viewModel.closeFinishedGame()

        startGame(targetScore = 10)
        viewModel.addPoints(10)
        history.save(engine.state.value!!.toMatchResult()!!) // what the auto-save would have done
        viewModel.discardResult()

        assertEquals(listOf(earlierResult.id), history.matches.value.keys.toList())
        assertNull(viewModel.uiState.value)
        assertEquals(listOf(GameEvent.NavigateHome, GameEvent.NavigateHome), events)
    }

    @Test
    fun abandonGame_endsWithoutSaving() = runTest {
        val events = collectEvents()
        startGame()
        viewModel.addPoint()
        viewModel.abandonGame()

        assertNull(viewModel.uiState.value)
        assertTrue(history.matches.value.isEmpty())
        assertEquals(listOf(GameEvent.NavigateHome), events)
    }
}
