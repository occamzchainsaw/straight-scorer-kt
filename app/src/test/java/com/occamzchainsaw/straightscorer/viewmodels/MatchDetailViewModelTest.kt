package com.occamzchainsaw.straightscorer.viewmodels

import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.PlayerMatchSummary
import com.occamzchainsaw.core.repositories.FakeMatchHistoryRepository
import com.occamzchainsaw.straightscorer.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMatchHistoryRepository()
    private val match = MatchResult(
        id = "m1",
        startedAt = 0,
        finishedAt = 1,
        targetScore = 50,
        racks = 3,
        players = listOf(PlayerMatchSummary(name = "Alice", finalScore = 50, isWinner = true))
    )

    @Test
    fun loadsMatch_andReportsWhenItIsGone() = runTest {
        repository.save(match)
        val viewModel = MatchDetailViewModel(repository, "m1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(MatchDetailUiState.Loaded(match), viewModel.uiState.value)

        repository.delete("m1")
        assertEquals(MatchDetailUiState.NotFound, viewModel.uiState.value)
    }
}
