package com.occamzchainsaw.straightscorer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.straightscorer.ui.components.playerEditCard
import com.occamzchainsaw.straightscorer.ui.navigation.Game
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.SetupEvent
import com.occamzchainsaw.straightscorer.viewmodels.SetupViewModel
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

@Composable
fun setupScreen(
    viewModel: SetupViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val players = uiState.players
    val playerErrors = uiState.playerErrors
    var showReplaceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SetupEvent.NavigateToGame -> navController?.navigate(Game) {
                    launchSingleTop = true
                }
                SetupEvent.ConfirmReplaceGame -> showReplaceDialog = true
            }
        }
    }

    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Start a new game?") },
            text = { Text("The game in progress will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceDialog = false
                    viewModel.confirmNewGame()
                }) { Text("Start new") }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) { Text("Cancel") }
            }
        )
    }

    var targetScoreValue by remember {
        mutableStateOf(TextFieldValue(uiState.targetScoreText))
    }
    if (targetScoreValue.text != uiState.targetScoreText) {
        targetScoreValue = TextFieldValue(uiState.targetScoreText)
    }
    var targetScoreTouched by remember { mutableStateOf(false) }
    val targetScoreError = uiState.targetScoreError.takeIf { targetScoreTouched || uiState.showErrors }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Game Setup",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column (
                modifier = Modifier.padding(12.dp, 4.dp, 12.dp, 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedRoundedSymbol(
                        icon = MaterialSymbols.SPORTS_SCORE,
                        size = 36.dp,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text (
                        "Target Score",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            if (focus.isFocused) {
                                targetScoreValue = targetScoreValue.copy(
                                    selection = TextRange(0, targetScoreValue.text.length)
                                )
                            }
                        },
                    value = targetScoreValue,
                    onValueChange = { newValue ->
                        if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() }) {
                            targetScoreValue = newValue
                            targetScoreTouched = true
                            viewModel.updateTargetScore(newValue.text)
                        }
                    },
                    isError = targetScoreError != null,
                    supportingText = targetScoreError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Players", style = MaterialTheme.typography.titleLarge)
            Text(
                "${players.size} / ${MatchSetup.MAX_PLAYERS}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = players, key = { it.id }) { player ->
                playerEditCard(
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(durationMillis = 150),
                        placementSpec = spring(stiffness = Spring.StiffnessMedium),
                        fadeOutSpec = tween(durationMillis = 100)
                    ),
                    item = player,
                    onItemUpdated = viewModel::updatePlayer,
                    nameError = playerErrors[player.id]?.name?.takeIf { uiState.showErrors || player.name.isNotEmpty() },
                    headStartError = playerErrors[player.id]?.headStart,
                    onDelete = if (uiState.canRemovePlayer) {
                        { viewModel.removePlayer(player.id) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = viewModel::addPlayer,
                modifier = Modifier.weight(1f),
                enabled = uiState.canAddPlayer
            ) {
                OutlinedRoundedSymbol(MaterialSymbols.PERSON_ADD)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Player")
            }
            if (uiState.isGameInProgress) {
                OutlinedButton(
                    onClick = viewModel::startGame,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("New Game")
                }
            }
            Button(
                onClick = if (uiState.isGameInProgress) viewModel::resumeGame else viewModel::startGame,
                modifier = Modifier.weight(1f)
            ) {
                Text(when (uiState.isGameInProgress) {
                    true -> "Resume Game"
                    else -> "Start Game"
                })
                Spacer(modifier = Modifier.width(6.dp))
                FilledRoundedSymbol(MaterialSymbols.PLAY_ARROW)
            }
        }
    }
}
