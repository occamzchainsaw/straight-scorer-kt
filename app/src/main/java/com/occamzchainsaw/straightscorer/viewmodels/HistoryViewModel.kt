package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.repositories.MatchHistoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { All, Matches, Practice }

data class HistoryUiState(
    val matches: List<MatchResult> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.All,
    val isLoading: Boolean = true
) {
    val canClear: Boolean
        get() = matches.isNotEmpty()

    /** [matches] narrowed down by [filter], newest first. */
    val visibleMatches: List<MatchResult>
        get() = when (filter) {
            HistoryFilter.All -> matches
            HistoryFilter.Matches -> matches.filterNot { it.isPractice }
            HistoryFilter.Practice -> matches.filter { it.isPractice }
        }
}

sealed interface HistoryEvent {
    /** Show an undo option (e.g. a snackbar) - call [HistoryViewModel.undoDelete] to restore. */
    data class MatchesDeleted(val count: Int) : HistoryEvent
}

class HistoryViewModel(private val repository: MatchHistoryRepository) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.All)

    val uiState: StateFlow<HistoryUiState> = combine(repository.observeMatches(), filter) { matches, filter ->
        HistoryUiState(matches = matches, filter = filter, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }

    private val _events = Channel<HistoryEvent>(Channel.BUFFERED)
    val events: Flow<HistoryEvent> = _events.receiveAsFlow()

    private var lastDeleted: List<MatchResult> = emptyList()

    fun deleteMatch(id: String) {
        viewModelScope.launch {
            val match = repository.getMatch(id) ?: return@launch
            repository.delete(id)
            lastDeleted = listOf(match)
            _events.send(HistoryEvent.MatchesDeleted(1))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val all = uiState.value.matches.ifEmpty { return@launch }
            repository.clear()
            lastDeleted = all
            _events.send(HistoryEvent.MatchesDeleted(all.size))
        }
    }

    fun undoDelete() {
        val restore = lastDeleted
        lastDeleted = emptyList()
        viewModelScope.launch { restore.forEach { repository.save(it) } }
    }
}
