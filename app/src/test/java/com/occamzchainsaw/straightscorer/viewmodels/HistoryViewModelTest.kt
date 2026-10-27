package com.occamzchainsaw.straightscorer.viewmodels

import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.PlayerMatchSummary
import com.occamzchainsaw.core.repositories.FakeMatchHistoryRepository
import com.occamzchainsaw.straightscorer.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMatchHistoryRepository()
    private val viewModel by lazy { HistoryViewModel(repository) }

    private fun result(id: String, finishedAt: Long) = MatchResult(
        id = id,
        startedAt = 0,
        finishedAt = finishedAt,
        targetScore = 50,
        racks = 3,
        players = listOf(PlayerMatchSummary(name = "Alice", finalScore = 50, isWinner = true))
    )

    private fun TestScope.observe(): List<HistoryEvent> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val events = mutableListOf<HistoryEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    private val matchIds get() = viewModel.uiState.value.matches.map { it.id }

    @Test
    fun matches_newestFirst() = runTest {
        repository.save(result("old", 1))
        repository.save(result("new", 2))
        observe()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("new", "old"), matchIds)
    }

    @Test
    fun deleteMatch_canBeUndone() = runTest {
        repository.save(result("a", 1))
        repository.save(result("b", 2))
        val events = observe()

        viewModel.deleteMatch("a")
        assertEquals(listOf("b"), matchIds)
        assertEquals(listOf(HistoryEvent.MatchesDeleted(1)), events)

        viewModel.undoDelete()
        assertEquals(listOf("b", "a"), matchIds)
    }

    @Test
    fun clearHistory_canBeUndone() = runTest {
        repository.save(result("a", 1))
        repository.save(result("b", 2))
        val events = observe()

        viewModel.clearHistory()
        assertTrue(matchIds.isEmpty())
        assertFalse(viewModel.uiState.value.canClear)
        assertEquals(listOf(HistoryEvent.MatchesDeleted(2)), events)

        viewModel.undoDelete()
        assertEquals(listOf("b", "a"), matchIds)
    }

    @Test
    fun undoDelete_onlyRestoresOnce() = runTest {
        repository.save(result("a", 1))
        observe()

        viewModel.deleteMatch("a")
        viewModel.undoDelete()
        repository.delete("a")
        viewModel.undoDelete()

        assertTrue(matchIds.isEmpty())
    }

    @Test
    fun filter_narrowsVisibleMatches() = runTest {
        repository.save(result("match", 2))
        repository.save(result("practice", 1).copy(targetScore = null))
        observe()

        viewModel.setFilter(HistoryFilter.Practice)
        assertEquals(listOf("practice"), viewModel.uiState.value.visibleMatches.map { it.id })

        viewModel.setFilter(HistoryFilter.Matches)
        assertEquals(listOf("match"), viewModel.uiState.value.visibleMatches.map { it.id })

        viewModel.setFilter(HistoryFilter.All)
        assertEquals(2, viewModel.uiState.value.visibleMatches.size)
    }
}
