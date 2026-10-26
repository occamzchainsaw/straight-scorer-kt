package com.occamzchainsaw.straightscorer.viewmodels

import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.services.GameEngine
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
class SetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val engine = GameEngine()
    private val settings = FakeSettingsRepository(AppSettings(defaultTargetScore = 75))
    private val viewModel by lazy { SetupViewModel(engine, settings) }

    private val state get() = viewModel.uiState.value
    private fun player(index: Int) = state.players[index]

    private fun TestScope.collectEvents(): List<SetupEvent> {
        val events = mutableListOf<SetupEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    @Test
    fun defaults_twoPlayers_firstBreaks_targetFromSettings() {
        assertEquals(listOf("Player 1", "Player 2"), state.players.map { it.name })
        assertEquals(listOf(true, false), state.players.map { it.isStarting })
        assertEquals("75", state.targetScoreText)
        assertTrue(state.isValid)
        assertFalse(state.isGameInProgress)
    }

    @Test
    fun targetScore_isValidated() {
        viewModel.updateTargetScore("9")
        assertEquals("Minimum target score is 10", state.targetScoreError)

        viewModel.updateTargetScore("1001")
        assertEquals("Maximum target score is 1000", state.targetScoreError)

        viewModel.updateTargetScore("")
        assertFalse(state.isValid)

        viewModel.updateTargetScore("12a")
        assertEquals("", state.targetScoreText)
    }

    @Test
    fun playerNames_areValidated() {
        viewModel.updatePlayer(player(0).copy(name = "  "))
        assertEquals("Name required", state.playerErrors[player(0).id]?.name)

        viewModel.updatePlayer(player(0).copy(name = "x".repeat(MatchSetup.MAX_NAME_LENGTH + 1)))
        assertEquals("Maximum 25 characters", state.playerErrors[player(0).id]?.name)

        viewModel.updatePlayer(player(0).copy(name = "player 2 "))
        assertEquals("Name already used", state.playerErrors[player(0).id]?.name)
        assertEquals("Name already used", state.playerErrors[player(1).id]?.name)
        assertFalse(state.isValid)
    }

    @Test
    fun headStart_mustBeBelowTarget() {
        viewModel.updateTargetScore("50")
        viewModel.updatePlayer(player(1).copy(headStart = "50"))
        assertEquals("Must be less than the target score", state.playerErrors[player(1).id]?.headStart)

        viewModel.updatePlayer(player(1).copy(headStart = ""))
        assertNull(state.playerErrors[player(1).id]?.headStart)
        assertTrue(state.isValid)
    }

    @Test
    fun startingPlayer_isExclusive_andCannotBeUnset() {
        viewModel.updatePlayer(player(1).copy(isStarting = true))
        assertEquals(listOf(false, true), state.players.map { it.isStarting })

        viewModel.updatePlayer(player(1).copy(isStarting = false))
        assertEquals(listOf(false, true), state.players.map { it.isStarting })

        viewModel.setStartingPlayer(player(0).id)
        assertEquals(listOf(true, false), state.players.map { it.isStarting })
    }

    @Test
    fun removingStartingPlayer_passesBreakToFirst() {
        viewModel.addPlayer()
        viewModel.setStartingPlayer(player(1).id)
        viewModel.removePlayer(player(1).id)

        assertEquals(listOf("Player 1", "Player 3"), state.players.map { it.name })
        assertTrue(player(0).isStarting)
    }

    @Test
    fun cannotRemoveLastPlayer() {
        viewModel.removePlayer(player(1).id)
        viewModel.removePlayer(player(0).id)

        assertEquals(1, state.players.size)
        assertFalse(state.canRemovePlayer)
    }

    @Test
    fun addPlayer_usesFreeName_andStopsAtMax() {
        viewModel.removePlayer(player(0).id) // leaves "Player 2"
        viewModel.addPlayer()
        assertEquals(listOf("Player 2", "Player 3"), state.players.map { it.name })

        repeat(20) { viewModel.addPlayer() }
        assertEquals(MatchSetup.MAX_PLAYERS, state.players.size)
        assertFalse(state.canAddPlayer)
        assertTrue(state.isValid)
    }

    @Test
    fun movePlayer_changesTurnOrder() {
        viewModel.addPlayer()
        viewModel.movePlayer(from = 2, to = 0)

        assertEquals(listOf("Player 3", "Player 1", "Player 2"), state.players.map { it.name })
    }

    @Test
    fun invalidStart_showsErrors_andStartsNothing() = runTest {
        val events = collectEvents()
        viewModel.updatePlayer(player(0).copy(name = ""))
        viewModel.startGame()

        assertTrue(state.showErrors)
        assertNull(engine.state.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun validStart_startsEngine_inListOrder() = runTest {
        val events = collectEvents()
        viewModel.updatePlayer(player(1).copy(name = " Bob ", headStart = "10", isStarting = true))
        viewModel.updateTargetScore("60")
        viewModel.startGame()

        val game = engine.state.value!!
        assertEquals(listOf("Player 1", "Bob"), game.players.map { it.name })
        assertEquals(10, game.players[1].score)
        assertEquals(1, game.atTableIndex)
        assertEquals(60, game.targetScore)
        assertEquals(listOf(SetupEvent.NavigateToGame), events)
        assertTrue(state.isGameInProgress)
    }

    @Test
    fun startWhileGameInProgress_asksFirst() = runTest {
        val events = collectEvents()
        viewModel.startGame()
        engine.addPoints(5)
        val firstGameId = engine.state.value!!.id

        viewModel.startGame()
        assertEquals(SetupEvent.ConfirmReplaceGame, events.last())
        assertEquals(firstGameId, engine.state.value!!.id)

        viewModel.confirmNewGame()
        assertEquals(SetupEvent.NavigateToGame, events.last())
        assertEquals(0, engine.state.value!!.players[0].score)
    }

    @Test
    fun resumeGame_onlyNavigatesWhenGameInProgress() = runTest {
        val events = collectEvents()
        viewModel.resumeGame()
        assertTrue(events.isEmpty())

        viewModel.startGame()
        viewModel.resumeGame()
        assertEquals(listOf(SetupEvent.NavigateToGame, SetupEvent.NavigateToGame), events)
    }

    @Test
    fun finishedGame_isNotInProgress() {
        viewModel.updateTargetScore("10")
        viewModel.startGame()
        engine.addPoints(10)

        assertFalse(state.isGameInProgress)
    }
}
