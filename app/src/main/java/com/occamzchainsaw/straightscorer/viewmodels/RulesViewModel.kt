package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occamzchainsaw.straightscorer.data.rules.RulesDocument
import com.occamzchainsaw.straightscorer.data.rules.RulesRepository
import com.occamzchainsaw.straightscorer.data.rules.SearchMatch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RulesSearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val matches: List<SearchMatch> = emptyList(),
    val currentIndex: Int = 0
) {
    val currentMatch: SearchMatch?
        get() = matches.getOrNull(currentIndex)
}

sealed interface RulesUiState {
    data object Loading : RulesUiState
    data class Loaded(val document: RulesDocument, val search: RulesSearchState = RulesSearchState()) : RulesUiState
    data object Error : RulesUiState
}

class RulesViewModel(
    private val repository: RulesRepository,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _uiState = MutableStateFlow<RulesUiState>(RulesUiState.Loading)
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = try {
                val markdown = repository.loadRules()
                RulesUiState.Loaded(withContext(parseDispatcher) { RulesDocument.parse(markdown) })
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                RulesUiState.Error
            }
        }
    }

    fun openSearch() = updateSearch { it.copy(isActive = true) }

    fun closeSearch() = updateSearch { RulesSearchState() }

    fun setQuery(query: String) {
        val document = (_uiState.value as? RulesUiState.Loaded)?.document ?: return
        updateSearch { it.copy(query = query, matches = document.search(query), currentIndex = 0) }
    }

    fun nextMatch() = updateSearch { search ->
        if (search.matches.isEmpty()) search
        else search.copy(currentIndex = (search.currentIndex + 1) % search.matches.size)
    }

    fun previousMatch() = updateSearch { search ->
        if (search.matches.isEmpty()) search
        else search.copy(currentIndex = (search.currentIndex - 1 + search.matches.size) % search.matches.size)
    }

    private fun updateSearch(transform: (RulesSearchState) -> RulesSearchState) {
        _uiState.update { state ->
            if (state is RulesUiState.Loaded) state.copy(search = transform(state.search)) else state
        }
    }
}
