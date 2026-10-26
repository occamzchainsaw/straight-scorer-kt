package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.core.models.GameStatus
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.rules.toMatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {
    private var settings = GameSettings()
    private lateinit var engine: GameEngine

    private val setup = MatchSetup(
        players = listOf(PlayerSetup(" Alice "), PlayerSetup("Bob", headStart = 20)),
        targetScore = 50,
        startingPlayerIndex = 1
    )

    @Before
    fun setUp() {
        engine = GameEngine(settings = { settings }, clock = { 1000L })
    }

    @Test
    fun noGame_untilStarted() {
        assertNull(engine.state.value)
        engine.addPoints()
        assertNull(engine.state.value)
    }

    @Test
    fun startGame_createsPlayersFromSetup() {
        engine.startGame(setup)

        val state = engine.state.value!!
        assertEquals(listOf("Alice", "Bob"), state.players.map { it.name })
        assertEquals(listOf(0, 1), state.players.map { it.id })
        assertEquals(20, state.players[1].score)
        assertEquals(1, state.atTableIndex)
        assertEquals(50, state.targetScore)
        assertEquals(1000L, state.startedAt)
        assertTrue(state.table.isOpeningBreak)
        assertFalse(engine.canUndo.value)
    }

    @Test
    fun startGame_rejectsInvalidSetup() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.startGame(setup.copy(targetScore = 5))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.startGame(setup.copy(players = listOf(PlayerSetup("A", headStart = 50))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.startGame(setup.copy(startingPlayerIndex = 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.startGame(setup.copy(players = listOf(PlayerSetup("Bob"), PlayerSetup(" bob"))))
        }
    }

    @Test
    fun restore_onlyWhenNoGameIsRunning() {
        engine.startGame(setup)
        val saved = engine.state.value!!
        engine.miss()

        assertFalse(engine.restore(saved))
        engine.clear()
        assertTrue(engine.restore(saved))
        assertEquals(saved, engine.state.value)
        assertFalse(engine.canUndo.value)
    }

    @Test
    fun undo_revertsActionsInOrder() {
        engine.startGame(setup)
        val initial = engine.state.value
        engine.addPoints(3)
        val afterPoints = engine.state.value
        engine.foul()

        engine.undo()
        assertEquals(afterPoints, engine.state.value)
        engine.undo()
        assertEquals(initial, engine.state.value)
        assertFalse(engine.canUndo.value)
    }

    @Test
    fun undo_reopensFinishedGame() {
        engine.startGame(setup)
        engine.addPoints(30)
        assertEquals(GameStatus.Finished(winnerId = 1), engine.state.value!!.status)

        engine.undo()
        assertEquals(GameStatus.InProgress, engine.state.value!!.status)
    }

    @Test
    fun ignoredActions_doNotCreateUndoSteps() {
        engine.startGame(setup)
        engine.miss()
        engine.breakingFoul(reBreak = false) // not on the opening break any more
        engine.undo()

        assertFalse(engine.canUndo.value)
    }

    @Test
    fun undoHistory_isCapped() {
        engine = GameEngine(clock = { 0L }, maxUndoSteps = 3)
        engine.startGame(setup)
        repeat(5) { engine.miss() }
        repeat(3) { engine.undo() }

        assertFalse(engine.canUndo.value)
        assertEquals(2, engine.state.value!!.breaks.size)
    }

    @Test
    fun startingNewGame_clearsUndoHistory() {
        engine.startGame(setup)
        engine.miss()
        engine.startGame(setup)

        assertFalse(engine.canUndo.value)
        assertTrue(engine.state.value!!.breaks.isEmpty())
    }

    @Test
    fun settings_areReadOnEveryAction() {
        engine.startGame(setup)
        settings = GameSettings(countMissesInAverage = false)
        engine.miss()

        assertEquals(0, engine.state.value!!.players[1].breakCount)
    }

    @Test
    fun clear_removesGame() {
        engine.startGame(setup)
        engine.miss()
        engine.clear()

        assertNull(engine.state.value)
        assertFalse(engine.canUndo.value)
    }

    @Test
    fun finishedGame_mapsToMatchResult() {
        engine.startGame(setup)
        assertNull(engine.state.value!!.toMatchResult())

        engine.addPoints(30)
        val result = engine.state.value!!.toMatchResult()

        assertNotNull(result)
        assertEquals("Bob", result!!.winner?.name)
        assertEquals(50, result.players[1].finalScore)
        assertEquals(20, result.players[1].headStart)
        assertEquals(30, result.players[1].highestBreak)
        assertEquals(3, result.racks)
        assertEquals(1, result.breaks.size)
    }

    @Test
    fun singlePlayer_startsPractice_ignoringTargetAndHeadStart() {
        engine.startGame(MatchSetup(listOf(PlayerSetup("Solo", headStart = 30)), targetScore = 5))

        val state = engine.state.value!!
        assertTrue(state.isPractice)
        assertNull(state.targetScore)
        assertEquals(0, state.players[0].score)
    }

    @Test
    fun finishedPractice_mapsToResultWithoutWinner() {
        engine.startGame(MatchSetup(listOf(PlayerSetup("Solo")), targetScore = 100))
        engine.addPoints(12)
        engine.finishPractice()

        val result = engine.state.value!!.toMatchResult()!!
        assertTrue(result.isPractice)
        assertNull(result.winner)
        assertEquals(12, result.players.single().highestBreak)
    }
}
