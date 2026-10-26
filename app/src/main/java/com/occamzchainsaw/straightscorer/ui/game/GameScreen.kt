package com.occamzchainsaw.straightscorer.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.GameEvent
import com.occamzchainsaw.straightscorer.viewmodels.GameViewModel

@Composable
fun GameScreen(
    onNavigateHome: () -> Unit,
    viewModel: GameViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val navigateHome by rememberUpdatedState(onNavigateHome)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GameEvent.NavigateHome -> navigateHome()
            }
        }
    }

    val state = uiState
    when {
        state != null -> {
            KeepScreenOn(state.keepScreenOn)
            var showAddPoints by rememberSaveable { mutableStateOf(false) }
            val haptics = LocalHapticFeedback.current
            val actions = remember(viewModel, haptics) {
                fun tap(type: HapticFeedbackType, action: () -> Unit): () -> Unit = {
                    haptics.performHapticFeedback(type)
                    action()
                }
                GameActions(
                    onBack = { navigateHome() },
                    onUndo = viewModel::undo,
                    onAbandon = viewModel::abandonGame,
                    onAddPoint = tap(HapticFeedbackType.Confirm, viewModel::addPoint),
                    onAddPoints = { points ->
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.addPoints(points)
                    },
                    onShowAddPoints = { showAddPoints = true },
                    onDismissAddPoints = { showAddPoints = false },
                    onSafe = tap(HapticFeedbackType.ContextClick, viewModel::safe),
                    onMiss = tap(HapticFeedbackType.ContextClick, viewModel::miss),
                    onFoul = tap(HapticFeedbackType.Reject, viewModel::foul),
                    onBreakShot = tap(HapticFeedbackType.ContextClick, viewModel::breakShot),
                    onFinishPractice = viewModel::finishPractice,
                    onResolveOpeningFoul = viewModel::resolveOpeningBreakFoul,
                    onDismissOpeningFoul = viewModel::dismissOpeningBreakFoulDialog,
                    onDone = viewModel::closeFinishedGame,
                    onDiscard = viewModel::discardResult
                )
            }
            GameContent(state = state, actions = actions, showAddPointsSheet = showAddPoints)
        }
        isRestoring -> RestoringGame()
        // no game to show, e.g. it was ended from somewhere else
        else -> LaunchedEffect(Unit) { navigateHome() }
    }
}

@Composable
private fun RestoringGame() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.game_restoring), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
