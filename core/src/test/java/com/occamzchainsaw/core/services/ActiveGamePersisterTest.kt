package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.repositories.FakeActiveGameStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveGamePersisterTest {
    private val setup = MatchSetup(players = listOf(PlayerSetup("Alice"), PlayerSetup("Bob")), targetScore = 50)

    private fun persisterTest(
        engine: GameEngine,
        store: FakeActiveGameStore,
        block: suspend TestScope.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        val job = launch { ActiveGamePersister(engine, store).run() }
        block()
        job.cancel()
    }

    private fun savedGame() = GameEngine().apply {
        startGame(setup)
        addPoints(7)
        miss()
    }.state.value!!

    @Test
    fun savedGame_isRestoredOnStart() {
        val saved = savedGame()
        val engine = GameEngine()
        persisterTest(engine, FakeActiveGameStore(saved)) {
            assertEquals(saved, engine.state.value)
        }
    }

    @Test
    fun everyChange_isSaved() {
        val engine = GameEngine()
        val store = FakeActiveGameStore()
        persisterTest(engine, store) {
            engine.startGame(setup)
            engine.addPoints(3)

            assertEquals(engine.state.value, store.saved)
        }
    }

    @Test
    fun clearedGame_removesSave() {
        val engine = GameEngine()
        val store = FakeActiveGameStore(savedGame())
        persisterTest(engine, store) {
            engine.clear()

            assertNull(store.saved)
        }
    }

    @Test
    fun unreadableSave_startsFresh() {
        val engine = GameEngine()
        val store = FakeActiveGameStore(savedGame()).apply { failOnLoad = true }
        persisterTest(engine, store) {
            assertNull(engine.state.value)
        }
    }

    @Test
    fun gameStartedBeforeRestore_isKept() {
        val engine = GameEngine().apply { startGame(setup.copy(targetScore = 75)) }
        persisterTest(engine, FakeActiveGameStore(savedGame())) {
            assertEquals(75, engine.state.value!!.targetScore)
        }
    }

    @Test
    fun isRestoring_untilLoadFinished() {
        val engine = GameEngine()
        val persister = ActiveGamePersister(engine, FakeActiveGameStore(savedGame()))
        assertEquals(true, persister.isRestoring.value)

        runTest(UnconfinedTestDispatcher()) {
            val job = launch { persister.run() }
            assertEquals(false, persister.isRestoring.value)
            job.cancel()
        }
    }
}
