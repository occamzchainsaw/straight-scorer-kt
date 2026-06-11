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
import androidx.compose.runtime.mutableStateListOf
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
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.straightscorer.dtos.PlayerSetupItem
import com.occamzchainsaw.straightscorer.ui.components.playerEditCard
import com.occamzchainsaw.straightscorer.ui.navigation.Game
import com.occamzchainsaw.straightscorer.viewmodels.GameViewModel
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

private const val MAX_PLAYERS = 10

@Composable
fun setupScreen(viewModel: GameViewModel = viewModel(), navController: NavController? = null) {
    val isGameInProgress by viewModel.isMatchActive.collectAsState()

    val players = remember {
        mutableStateListOf(
            PlayerSetupItem(isStarting = true),
            PlayerSetupItem()
        )
    }
    var targetScoreValue by remember {
        mutableStateOf(TextFieldValue("100"))
    }
    var targetScoreTouched by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }

    val targetScore = targetScoreValue.text.toIntOrNull() ?: 0
    val targetScoreIsError = (targetScoreTouched || attempted) && targetScore < 10
    val allNamesValid = players.all { it.name.isNotBlank() }

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
                        }
                    },
                    isError = targetScoreIsError,
                    supportingText = if (targetScoreIsError) {
                        { Text("Minimum target score is 10") }
                    } else null,
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
                "${players.size} / $MAX_PLAYERS",
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
                    onItemUpdated = { updatedPlayer ->
                        val index = players.indexOfFirst { it.id == updatedPlayer.id }
                        if (index != -1) {
                            if (updatedPlayer.isStarting && !player.isStarting) {
                                for (i in players.indices) {
                                    players[i] = players[i].copy(isStarting = false)
                                }
                            }
                            players[index] = updatedPlayer
                        }
                    },
                    showErrors = attempted,
                    onDelete = if (players.size > 1) {
                        {
                            players.remove(player)
                            if (players.none { it.isStarting })
                                players[0] = players[0].copy(isStarting = true)
                        }
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
                onClick = { players.add(PlayerSetupItem()) },
                modifier = Modifier.weight(1f),
                enabled = players.size < MAX_PLAYERS
            ) {
                OutlinedRoundedSymbol(MaterialSymbols.PERSON_ADD)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Player")
            }
            Button(
                onClick = {
                    if (!isGameInProgress && (targetScore >= 10 && allNamesValid)) {
                        viewModel.start(
                            players.map {
                                Player(
                                    name = it.name,
                                    isStarting = it.isStarting,
                                    headStart = it.headStart.toIntOrNull() ?: 0
                                )
                            },
                            targetScore
                        )
                        navController?.navigate(Game) {
                            launchSingleTop = true
                        }
                    } else if (isGameInProgress) {
                        navController?.navigate(Game) {
                            restoreState = true
                            launchSingleTop = true
                        }
                    } else {
                        attempted = true
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(when (isGameInProgress) {
                    true -> "Resume Game"
                    else -> "Start Game"
                })
                Spacer(modifier = Modifier.width(6.dp))
                FilledRoundedSymbol(MaterialSymbols.PLAY_ARROW)
            }
        }
    }
}
