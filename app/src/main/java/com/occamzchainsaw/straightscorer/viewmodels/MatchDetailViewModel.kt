package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.repositories.MatchHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MatchDetailUiState {
    data object Loading : MatchDetailUiState
    data class Loaded(val match: MatchResult) : MatchDetailUiState

    /** Deleted in the meantime, or a stale link. */
    data object NotFound : MatchDetailUiState
}

/** Read-only view of one saved match. Deleting goes through the history screen so it can offer undo. */
class MatchDetailViewModel(
    repository: MatchHistoryRepository,
    val matchId: String
) : ViewModel() {

    val uiState: StateFlow<MatchDetailUiState> = repository.observeMatches()
        .map { matches ->
            matches.firstOrNull { it.id == matchId }
                ?.let { MatchDetailUiState.Loaded(it) }
                ?: MatchDetailUiState.NotFound
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchDetailUiState.Loading)
}
