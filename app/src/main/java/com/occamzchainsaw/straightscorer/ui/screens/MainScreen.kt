package com.occamzchainsaw.straightscorer.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.straightscorer.viewmodels.GameViewModel

@Composable
fun MainScreen(viewModel: GameViewModel = viewModel()) {
    val player1 = Player(name = "player1", isStarting = true, isAtTable = true,)
    val player2 = Player(name = "player2")
    val newPlayers = listOf(player1, player2)
    viewModel.setup(newPlayers, 100)
    val players by viewModel.players.collectAsState()
    val breakHistory by viewModel.breakHistory.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()
    val matchResult by viewModel.matchResult.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column (modifier = Modifier.fillMaxWidth()) {
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
                Column (modifier = Modifier.padding(0.dp, 2.dp).fillMaxWidth()) {
                    Text("Current Break", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Row (modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(players.first { it.isAtTable }.name)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${players.first { it.isStarting }.currentBreak}",
                            modifier = Modifier.align(Alignment.End))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}