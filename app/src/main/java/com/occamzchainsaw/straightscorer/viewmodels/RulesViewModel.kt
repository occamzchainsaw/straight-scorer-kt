package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occamzchainsaw.straightscorer.data.rules.RulesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RulesUiState {
    data object Loading : RulesUiState
    data class Loaded(val markdown: String) : RulesUiState
    data object Error : RulesUiState
}

class RulesViewModel(private val repository: RulesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<RulesUiState>(RulesUiState.Loading)
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = runCatching { repository.loadRules() }
                .fold(onSuccess = { RulesUiState.Loaded(it) }, onFailure = { RulesUiState.Error })
        }
    }
}
