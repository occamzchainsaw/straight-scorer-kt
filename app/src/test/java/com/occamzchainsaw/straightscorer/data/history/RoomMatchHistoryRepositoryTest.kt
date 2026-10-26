package com.occamzchainsaw.straightscorer.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.PlayerMatchSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class RoomMatchHistoryRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomMatchHistoryRepository

    private fun result(id: String, finishedAt: Long, winner: String = "Alice") = MatchResult(
        id = id,
        startedAt = finishedAt - 1000,
        finishedAt = finishedAt,
        targetScore = 50,
        racks = 4,
        players = listOf(
            PlayerMatchSummary(name = "Alice", finalScore = 50, highestBreak = 22, averageBreak = 7.5f,
                breakCount = 6, totalFouls = 1, isWinner = winner == "Alice"),
            PlayerMatchSummary(name = "Bob", headStart = 10, finalScore = 31, isWinner = winner == "Bob")
        ),
        breaks = listOf(
            Break(playerId = 0, playerName = "Alice", points = 0, penalty = 2,
                endAction = BreakEndAction.BreakingFoul, rackNumber = 1),
            Break(playerId = 0, playerName = "Alice", points = 22, endAction = BreakEndAction.Win, rackNumber = 4)
        )
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMatchHistoryRepository(database.matchDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun save_roundTripsAllFields() = runTest {
        val original = result("a", finishedAt = 2000)
        repository.save(original)

        assertEquals(original, repository.getMatch("a"))
    }

    @Test
    fun observeMatches_ordersNewestFirst() = runTest {
        repository.save(result("old", finishedAt = 1000))
        repository.save(result("new", finishedAt = 3000))
        repository.save(result("mid", finishedAt = 2000))

        assertEquals(listOf("new", "mid", "old"), repository.observeMatches().first().map { it.id })
    }

    @Test
    fun save_withSameId_replacesResult() = runTest {
        repository.save(result("a", finishedAt = 1000, winner = "Alice"))
        repository.save(result("a", finishedAt = 1500, winner = "Bob"))

        val matches = repository.observeMatches().first()
        assertEquals(1, matches.size)
        assertEquals("Bob", matches.single().winner?.name)
    }

    @Test
    fun delete_removesOnlyThatMatch() = runTest {
        repository.save(result("a", finishedAt = 1000))
        repository.save(result("b", finishedAt = 2000))
        repository.delete("a")

        assertNull(repository.getMatch("a"))
        assertEquals(listOf("b"), repository.observeMatches().first().map { it.id })
    }

    @Test
    fun clear_removesEverything() = runTest {
        repository.save(result("a", finishedAt = 1000))
        repository.save(result("b", finishedAt = 2000))
        repository.clear()

        assertTrue(repository.observeMatches().first().isEmpty())
    }

    @Test
    fun entity_storesWinnerNameColumn() {
        assertEquals("Bob", result("a", 1000, winner = "Bob").toEntity().winnerName)
    }
}
