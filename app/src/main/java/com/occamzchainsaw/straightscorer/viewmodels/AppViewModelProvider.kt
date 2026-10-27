package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.occamzchainsaw.straightscorer.AppContainer
import androidx.navigation.toRoute
import com.occamzchainsaw.straightscorer.StraightScorerApplication
import com.occamzchainsaw.straightscorer.ui.navigation.MatchDetail

/** Creates every ViewModel from the app's [AppContainer]. Use with `viewModel(factory = AppViewModelProvider.Factory)`. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { SetupViewModel(container().gameEngine, container().settingsRepository) }
        initializer {
            GameViewModel(
                container().gameEngine,
                container().matchRecorder,
                container().settingsRepository,
                container().isRestoringGame
            )
        }
        initializer { HistoryViewModel(container().matchHistoryRepository) }
        initializer {
            MatchDetailViewModel(
                container().matchHistoryRepository,
                createSavedStateHandle().toRoute<MatchDetail>().matchId
            )
        }
        initializer { SettingsViewModel(container().settingsRepository) }
        initializer { RulesViewModel(container().rulesRepository) }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as StraightScorerApplication).container
}
