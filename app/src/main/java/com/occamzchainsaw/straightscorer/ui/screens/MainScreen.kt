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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.straightscorer.dtos.PlayerSetupItem
import com.occamzchainsaw.straightscorer.ui.components.PlayerEditCard
import com.occamzchainsaw.straightscorer.ui.components.SwipeBackground
import com.occamzchainsaw.straightscorer.viewmodels.GameViewModel

@Composable
fun MainScreen(viewModel: GameViewModel = viewModel()) {
    val isMatchActive by viewModel.isMatchActive.collectAsState()

    if (isMatchActive) {
        GameContent(viewModel)
    } else {
        SetupContent(onStartClicked = { players, targetScore ->
            viewModel.start(players, targetScore)
        })
    }
}

@Composable
fun SetupContent(
    onStartClicked: (List<Player>, Int) -> Unit
) {
    var players = remember {
        mutableStateListOf(
            PlayerSetupItem(isStarting = true),
            PlayerSetupItem()
        )
    }
    var targetScoreText by remember { mutableStateOf("100") }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Setup your game")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            modifier = Modifier.padding(8.dp),
            value = targetScoreText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    targetScoreText = newValue
                }
            },
            label = { Text("Target Score") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Setup the players")
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = players, key = { it.id }) { player ->
                val dismissState = rememberSwipeToDismissBoxState(
                    initialValue = SwipeToDismissBoxValue.Settled,
                    positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    onDismiss = { dismissDirection ->
                        if (dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            players.remove(player)
                            if (players.none { it.isStarting })
                                players[0] = players[0].copy(isStarting = true)
                        }
                    },
                    backgroundContent = { SwipeBackground() }
                ) {
                    PlayerEditCard(
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
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            OutlinedButton(onClick = { players.add(PlayerSetupItem()) }) {
                Text("Add Player")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    onStartClicked(
                        players.map {
                            Player(
                                name = it.name,
                                isStarting = it.isStarting
                            )
                        },
                        targetScoreText.toIntOrNull() ?: 100
                    ) },
                enabled = players.isNotEmpty()
            ) {
                Text("Start Game")
            }
        }
    }
}

@Composable
fun GameContent(viewModel: GameViewModel) {
    val players by viewModel.players.collectAsState()
    val breakHistory by viewModel.breakHistory.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()
    val matchResult by viewModel.matchResult.collectAsState()

    if (players.size >= 2) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row {
                    Card(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Column(modifier = Modifier.padding(8.dp).align(Alignment.Start)) {
                            Text(players[0].name, modifier = Modifier.align(Alignment.Start))
                            Text("${players[0].score}", modifier = Modifier.align(Alignment.Start))
                        }
                    }

                    Card(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Column(modifier = Modifier.padding(8.dp).align(Alignment.End)) {
                            Text(players[1].name, modifier = Modifier.align(Alignment.End))
                            Text("${players[1].score}", modifier = Modifier.align(Alignment.End))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card {
                    Column(modifier = Modifier.padding(0.dp, 2.dp).fillMaxWidth()) {
                        Text(
                            "Current Break",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    Row(modifier = Modifier.padding(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(players.first { it.isAtTable }.name)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${players.first { it.isStarting }.currentBreak}",
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        Text("Loading...")
    }
}