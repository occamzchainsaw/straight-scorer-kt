package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.repositories.FakeMatchHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchRecorderTest {
    private val engine = GameEngine(clock = { 5000L })
    private val repository = FakeMatchHistoryRepository()
    private val recorder = MatchRecorder(engine, repository)

    private val setup = MatchSetup(
        players = listOf(PlayerSetup("Alice"), PlayerSetup("Bob")),
        targetScore = 10
    )

    private fun recorderTest(block: suspend TestScope.() -> Unit) =
        runTest(UnconfinedTestDispatcher()) {
            val job = launch { recorder.run() }
            block()
            job.cancel()
        }

    @Test
    fun gameInProgress_isNotSaved() = recorderTest {
        engine.startGame(setup)
        engine.addPoints(5)

        assertTrue(repository.matches.value.isEmpty())
    }

    @Test
    fun finishedGame_isSavedOnce() = recorderTest {
        engine.startGame(setup)
        engine.addPoints(10)
        engine.miss() // ignored, game is over

        val saved = repository.matches.value.values.single()
        assertEquals(engine.state.value!!.id, saved.id)
        assertEquals("Alice", saved.winner?.name)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun undoingTheWin_removesSavedResult() = recorderTest {
        engine.startGame(setup)
        engine.addPoints(10)
        engine.undo()

        assertTrue(repository.matches.value.isEmpty())
    }

    @Test
    fun winningAgainAfterUndo_overwritesResult() = recorderTest {
        engine.startGame(setup)
        engine.addPoints(10)
        engine.undo()
        engine.miss()
        engine.addPoints(10)

        val saved = repository.matches.value.values.single()
        assertEquals("Bob", saved.winner?.name)
    }

    @Test
    fun discard_removesResult_andEndsGame() = recorderTest {
        engine.startGame(setup)
        engine.addPoints(10)
        recorder.discard()

        assertTrue(repository.matches.value.isEmpty())
        assertNull(engine.state.value)
    }

    @Test
    fun discard_doesNothingWhileGameInProgress() = recorderTest {
        engine.startGame(setup)
        recorder.discard()

        assertEquals(setup.targetScore, engine.state.value?.targetScore)
    }

    @Test
    fun newGame_afterFinishedOne_keepsOldResult() = recorderTest {
        engine.startGame(setup)
        engine.addPoints(10)
        engine.startGame(setup)
        engine.addPoints(3)

        assertEquals(1, repository.matches.value.size)
    }

    @Test
    fun close_savesResult_evenWithoutRunningObserver() = runTest {
        engine.startGame(setup)
        engine.addPoints(10)
        recorder.close()

        assertEquals(1, repository.matches.value.size)
        assertNull(engine.state.value)
    }

    @Test
    fun close_doesNothingWhileGameInProgress() = recorderTest {
        engine.startGame(setup)
        recorder.close()

        assertTrue(repository.matches.value.isEmpty())
        assertEquals(setup.targetScore, engine.state.value?.targetScore)
    }
}
