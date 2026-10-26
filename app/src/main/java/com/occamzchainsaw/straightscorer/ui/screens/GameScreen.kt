package com.occamzchainsaw.straightscorer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.models.TableState
import com.occamzchainsaw.straightscorer.ui.navigation.BottomNavItem
import com.occamzchainsaw.straightscorer.ui.navigation.GameSetup
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.GameEvent
import com.occamzchainsaw.straightscorer.viewmodels.GameViewModel
import com.occamzchainsaw.straightscorer.viewmodels.OpeningBreakFoul
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.FilledRoundedSymbol

private val ColorMiss = Color(0xFFF57C00)
private val ColorSafe = Color(0xFF1565C0)
private val ColorFoul = Color(0xFFD32F2F)
private val ColorThirdFoul = Color(0xFF7B0000)
private val ColorWin = Color(0xFFFFB300)

private fun breakActionColor(action: BreakEndAction) = when (action) {
    BreakEndAction.Miss -> ColorMiss
    BreakEndAction.Safe -> ColorSafe
    BreakEndAction.Foul, BreakEndAction.BreakingFoul -> ColorFoul
    BreakEndAction.ThirdFoul -> ColorThirdFoul
    BreakEndAction.Win -> ColorWin
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun gameScreen(
    navController: NavController,
    availableRoutes: List<BottomNavItem>,
    viewModel: GameViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val goHome = {
        if (!navController.popBackStack(GameSetup, inclusive = false)) {
            navController.navigate(GameSetup) { launchSingleTop = true }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GameEvent.NavigateHome -> goHome()
            }
        }
    }

    val state = uiState
    if (state == null) {
        // no game (e.g. ended elsewhere) - nothing to show here
        LaunchedEffect(Unit) { goHome() }
        return
    }

    val view = LocalView.current
    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    if (state.showOpeningBreakFoulDialog) {
        openingBreakFoulDialog(
            onChoice = viewModel::resolveOpeningBreakFoul,
            onDismiss = viewModel::dismissOpeningBreakFoulDialog
        )
    }
    if (state.isFinished) {
        gameFinishedDialog(
            winnerName = state.winner?.name.orEmpty(),
            canUndo = state.canUndo,
            onUndo = viewModel::undo,
            onDiscard = viewModel::discardResult,
            onClose = viewModel::closeFinishedGame
        )
    }

    var showAbandonDialog by remember { mutableStateOf(false) }
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Abandon game?") },
            text = { Text("The game will end without saving a result.") },
            confirmButton = {
                TextButton(onClick = {
                    showAbandonDialog = false
                    viewModel.abandonGame()
                }) { Text("Abandon") }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) { Text("Cancel") }
            }
        )
    }

    val players = state.players
    val breakHistory = state.breaks
    val targetScore = state.targetScore
    val tableState = state.table

    val isTwoPlayer = state.isHeadToHead
    val activePlayer = state.playerAtTable
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
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                        FilledRoundedSymbol(icon = MaterialSymbols.UNDO)
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            FilledRoundedSymbol(icon = MaterialSymbols.MORE_HORIZ)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (!state.isFinished) {
                                DropdownMenuItem(
                                    text = { Text("Abandon game") },
                                    leadingIcon = { FilledRoundedSymbol(MaterialSymbols.CLOSE) },
                                    onClick = {
                                        menuExpanded = false
                                        showAbandonDialog = true
                                    }
                                )
                            }
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
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Race to $targetScore",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )

            if (isTwoPlayer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEach { player ->
                        headToHeadPlayerCard(
                            player = player,
                            isAtTable = player.id == activePlayer.id,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    players.forEach { player ->
                        playerScoreCard(
                            player = player,
                            isAtTable = player.id == activePlayer.id,
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
                smallInfoCard("Break", activePlayer.currentBreak, Modifier.weight(1f))
                smallInfoCard("Rack", tableState.rackNumber, Modifier.weight(1f))
                smallInfoCard("Balls on table", tableState.ballsOnTable, Modifier.weight(1f))
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
                onAddPoint = viewModel::addPoint,
                onSafe = viewModel::safe,
                onMiss = viewModel::miss,
                onFoul = viewModel::foul
            )
        }
    }
}

@Composable
private fun headToHeadPlayerCard(
    player: Player,
    isAtTable: Boolean,
    modifier: Modifier = Modifier,
) {

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isAtTable)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.secondary,
        label = "borderColor"
    )

    val animatedBorderStroke by animateDpAsState(
        targetValue = if (isAtTable) 2.dp else 0.dp,
        label = "borderStroke"
    )

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isAtTable)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.surfaceDim,
        label = "containerColor"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isAtTable)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "textColor"
    )

    Card(
        modifier = modifier,
        border = BorderStroke(width = animatedBorderStroke, color = animatedBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor,
            contentColor = animatedContentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(player.consecutiveFouls) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }

                Text(
                    text = "${player.score}",
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
    }
}

@Composable
private fun playerScoreCard(
    player: Player,
    isAtTable: Boolean,
    modifier: Modifier = Modifier,
) {

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isAtTable)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.secondary,
        label = "borderColor"
    )

    val animatedBorderStroke by animateDpAsState(
        targetValue = if (isAtTable) 2.dp else 0.dp,
        label = "borderStroke"
    )

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isAtTable)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.surfaceDim,
        label = "containerColor"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isAtTable)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "textColor"
    )

    Card(
        modifier = modifier,
        border = BorderStroke(width = animatedBorderStroke, color = animatedBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor,
            contentColor = animatedContentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                player.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (player.consecutiveFouls > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(player.consecutiveFouls) {
                        Box(modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
            Text(
                text = "${player.score}",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.2f)
            )
        }
    }
}

@Composable
private fun <T : Any> smallInfoCard(
    header: String,
    data: T,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                header,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$data",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.End
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
    val actionColor = breakActionColor(breakItem.endAction)
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val isPlayer0 = isTwoPlayer && breakItem.playerId == players.getOrNull(0)?.id

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
    val actionLabel = when (breakItem.endAction) {
        BreakEndAction.Miss -> "Miss"
        BreakEndAction.Safe -> "Safe"
        BreakEndAction.Foul -> "Foul"
        BreakEndAction.BreakingFoul -> "Break foul"
        BreakEndAction.ThirdFoul -> "3× Foul"
        BreakEndAction.Win -> "Win!"
    }
    val color = breakActionColor(breakItem.endAction)
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    val alignment = if (alignEnd) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (showPlayer) {
            Text(
                breakItem.playerName,
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
        if (breakItem.netPoints != 0) {
            Text(
                "${if (breakItem.netPoints > 0) "+" else ""}${breakItem.netPoints}",
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

@Composable
private fun openingBreakFoulDialog(
    onChoice: (OpeningBreakFoul) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foul on the break") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Breaking foul (-2): the incoming player accepts the table or asks for a re-break.")
                TextButton(onClick = { onChoice(OpeningBreakFoul.BreakingAcceptTable) }) {
                    Text("Breaking foul - accept table")
                }
                TextButton(onClick = { onChoice(OpeningBreakFoul.BreakingReBreak) }) {
                    Text("Breaking foul - re-break")
                }
                TextButton(onClick = { onChoice(OpeningBreakFoul.Standard) }) {
                    Text("Standard foul (-1)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun gameFinishedDialog(
    winnerName: String,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDiscard: () -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("$winnerName wins!") },
        text = { Text("The result has been saved to the match history.") },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Done") }
        },
        dismissButton = {
            Row {
                if (canUndo) {
                    TextButton(onClick = onUndo) { Text("Undo") }
                }
                TextButton(onClick = onDiscard) { Text("Discard") }
            }
        }
    )
}
