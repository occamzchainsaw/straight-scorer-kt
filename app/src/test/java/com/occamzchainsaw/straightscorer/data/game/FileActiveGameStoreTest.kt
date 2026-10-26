package com.occamzchainsaw.straightscorer.data.game

import com.occamzchainsaw.core.models.GameStatus
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.services.GameEngine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileActiveGameStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun game(finish: Boolean) = GameEngine().apply {
        startGame(MatchSetup(listOf(PlayerSetup("Alice", headStart = 5), PlayerSetup("Bob")), targetScore = 20))
        addPoints(4)
        foul()
        breakingFoul(reBreak = false)
        if (finish) addPoints(20)
    }.state.value!!

    private fun runStoreTest(block: suspend (FileActiveGameStore, File) -> Unit) = runTest {
        val file = File(tempFolder.root, "game/active_game.json")
        block(FileActiveGameStore(file, StandardTestDispatcher(testScheduler)), file)
    }

    @Test
    fun missingFile_loadsNull() = runStoreTest { store, _ ->
        assertNull(store.load())
    }

    @Test
    fun gameInProgress_roundTrips() = runStoreTest { store, _ ->
        val game = game(finish = false)
        store.save(game)

        assertEquals(game, store.load())
    }

    @Test
    fun finishedGame_roundTrips() = runStoreTest { store, _ ->
        val game = game(finish = true)
        store.save(game)

        val loaded = store.load()
        assertEquals(game, loaded)
        assertEquals(GameStatus.Finished(winnerId = 1), loaded?.status)
    }

    @Test
    fun savingNull_deletesFile() = runStoreTest { store, file ->
        store.save(game(finish = false))
        store.save(null)

        assertFalse(file.exists())
        assertNull(store.load())
    }

    @Test(expected = Exception::class)
    fun corruptFile_throws() = runStoreTest { store, file ->
        file.parentFile!!.mkdirs()
        file.writeText("{ not json")
        store.load()
    }
}
