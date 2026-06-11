package com.occamzchainsaw.straightscorer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.models.TableState
import com.occamzchainsaw.straightscorer.ui.navigation.BottomNavItem
import com.occamzchainsaw.straightscorer.ui.navigation.GameSetup
import com.occamzchainsaw.straightscorer.viewmodels.GameViewModel
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol
import dev.vicart.compose.material.symbols.FilledRoundedSymbol

private val ColorMiss = Color(0xFFF57C00)
private val ColorSafe = Color(0xFF1565C0)
private val ColorFoul = Color(0xFFD32F2F)
private val ColorThirdFoul = Color(0xFF7B0000)
private val ColorWin = Color(0xFFFFB300)

private fun breakActionColor(action: BreakEndAction) = when (action) {
    BreakEndAction.Miss -> ColorMiss
    BreakEndAction.Safe -> ColorSafe
    BreakEndAction.Foul -> ColorFoul
    BreakEndAction.ThirdFoul -> ColorThirdFoul
    BreakEndAction.Win -> ColorWin
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun gameScreen(
    viewModel: GameViewModel,
    navController: NavController,
    availableRoutes: List<BottomNavItem>
) {
    val players by viewModel.players.collectAsState()
    val breakHistory by viewModel.breakHistory.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()
    val tableState by viewModel.tableState.collectAsState()

    val isTwoPlayer = players.size == 2
    val activePlayer = players.firstOrNull { it.isAtTable }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text("Straight Scorer")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(GameSetup) {
                            popUpTo<GameSetup> {
                                inclusive = false
                                saveState = true
                            }
                        }
                    }) {
                        FilledRoundedSymbol(icon = MaterialSymbols.ARROW_BACK)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            FilledRoundedSymbol(icon = MaterialSymbols.MORE_HORIZ)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            availableRoutes.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.title) },
                                    leadingIcon = {
                                        FilledRoundedSymbol(item.symbol)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Player scores
            if (isTwoPlayer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEach { player ->
                        playerScoreCard(
                            player = player,
                            targetScore = targetScore,
                            compact = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    players.forEach { player ->
                        playerScoreCard(
                            player = player,
                            targetScore = targetScore,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Current break + rack info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentBreakCard(activePlayer, Modifier.weight(1f))
                rackInfoCard(tableState, Modifier.weight(1f))
            }

            // History
            Text(
                "History",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(breakHistory.reversed()) { index, breakItem ->
                    breakHistoryRow(
                        breakItem = breakItem,
                        players = players,
                        isTwoPlayer = isTwoPlayer,
                        isFirst = index == 0,
                        isLast = index == breakHistory.size - 1
                    )
                }
            }

            // Action buttons
            gameActionButtons(
                onAddPoint = { viewModel.addPoints() },
                onSafe = { viewModel.safe() },
                onMiss = { viewModel.miss() },
                onFoul = { viewModel.foul() }
            )
        }
    }
}

@Composable
private fun playerScoreCard(
    player: Player,
    targetScore: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val isAtTable = player.isAtTable
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isAtTable)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed-width indicator slot so name is always aligned
                Box(modifier = Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                    if (isAtTable) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        player.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player.consecutiveFouls > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(player.consecutiveFouls) {
                                Box(
                                    Modifier.size(6.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }
                Text(
                    "${player.score}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    " / $targetScore",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                        if (isAtTable) {
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        player.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${player.score}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "/ $targetScore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (player.consecutiveFouls > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(player.consecutiveFouls) {
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun currentBreakCard(activePlayer: Player?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Current Break",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${activePlayer?.currentBreak ?: 0}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                activePlayer?.name ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rackInfoCard(tableState: TableState, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Table",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${tableState.ballsOnTable}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "balls · rack ${tableState.rackNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val HistoryRowHeight = 44.dp
private val GraphColumnWidth = 48.dp
private val NodeRadius = 7f   // dp, used inside Canvas

@Composable
private fun breakHistoryRow(
    breakItem: Break,
    players: List<Player>,
    isTwoPlayer: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    val actionColor = breakActionColor(breakItem.breakEndAction)
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val isPlayer0 = isTwoPlayer && breakItem.player.name == players.getOrNull(0)?.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HistoryRowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTwoPlayer) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isPlayer0) {
                    breakNodeInfo(breakItem = breakItem, alignEnd = true)
                }
            }
        }

        // Graph column
        Box(
            modifier = Modifier
                .width(GraphColumnWidth)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val nr = NodeRadius.dp.toPx()
                val sw = 2.dp.toPx()

                if (!isFirst) {
                    drawLine(lineColor, Offset(cx, 0f), Offset(cx, cy - nr), sw)
                }
                if (!isLast) {
                    drawLine(lineColor, Offset(cx, cy + nr), Offset(cx, size.height), sw)
                }
                drawCircle(actionColor, nr, Offset(cx, cy))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when {
                isTwoPlayer && !isPlayer0 -> breakNodeInfo(breakItem = breakItem, alignEnd = false)
                !isTwoPlayer -> breakNodeInfo(breakItem = breakItem, alignEnd = false, showPlayer = true)
            }
        }
    }
}

@Composable
private fun breakNodeInfo(
    breakItem: Break,
    alignEnd: Boolean,
    showPlayer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val actionLabel = when (breakItem.breakEndAction) {
        BreakEndAction.Miss -> "Miss"
        BreakEndAction.Safe -> "Safe"
        BreakEndAction.Foul -> "Foul"
        BreakEndAction.ThirdFoul -> "3× Foul"
        BreakEndAction.Win -> "Win!"
    }
    val color = breakActionColor(breakItem.breakEndAction)
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    val alignment = if (alignEnd) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (showPlayer) {
            Text(
                breakItem.player.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            actionLabel,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign
        )
        if (breakItem.pointsScored != 0) {
            Text(
                "${if (breakItem.pointsScored > 0) "+" else ""}${breakItem.pointsScored}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign
            )
        }
    }
}

@Composable
private fun gameActionButtons(
    onAddPoint: () -> Unit,
    onSafe: () -> Unit,
    onMiss: () -> Unit,
    onFoul: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onAddPoint, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("+1")
        }
        OutlinedButton(onClick = onSafe, modifier = Modifier.weight(1f)) {
            Text("Safe")
        }
        OutlinedButton(onClick = onMiss, modifier = Modifier.weight(1f)) {
            Text("Miss")
        }
        Button(
            onClick = onFoul,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Foul")
        }
    }
}
