package com.occamzchainsaw.straightscorer.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import com.occamzchainsaw.straightscorer.viewmodels.GameUiState
import com.occamzchainsaw.straightscorer.viewmodels.OpeningBreakFoul
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

@Immutable
data class GameActions(
    val onBack: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onAbandon: () -> Unit = {},
    val onAddPoint: () -> Unit = {},
    val onAddPoints: (Int) -> Unit = {},
    val onShowAddPoints: () -> Unit = {},
    val onDismissAddPoints: () -> Unit = {},
    val onSafe: () -> Unit = {},
    val onMiss: () -> Unit = {},
    val onFoul: () -> Unit = {},
    val onBreakShot: () -> Unit = {},
    val onFinishPractice: () -> Unit = {},
    val onResolveOpeningFoul: (OpeningBreakFoul) -> Unit = {},
    val onDismissOpeningFoul: () -> Unit = {},
    val onDone: () -> Unit = {},
    val onDiscard: () -> Unit = {}
)

private val WideLayoutMinWidth = 600.dp

/** The whole game screen for a given state - no ViewModel, so it can be previewed and screenshot-tested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContent(
    state: GameUiState,
    actions: GameActions,
    showAddPointsSheet: Boolean,
    modifier: Modifier = Modifier
) {
    var showAbandonDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            GameTopBar(
                targetScore = state.targetScore,
                canUndo = state.canUndo,
                canAbandon = !state.isFinished,
                canFinishPractice = state.isPractice && !state.isFinished,
                onBack = actions.onBack,
                onUndo = actions.onUndo,
                onAbandon = { showAbandonDialog = true },
                onFinishPractice = actions.onFinishPractice
            )
        }
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (maxWidth >= WideLayoutMinWidth) {
                WideGameLayout(state, actions)
            } else {
                CompactGameLayout(state, actions, scoreboardMaxHeight = maxHeight * 0.45f)
            }
        }
    }

    if (showAbandonDialog) {
        AbandonDialog(
            onConfirm = {
                showAbandonDialog = false
                actions.onAbandon()
            },
            onDismiss = { showAbandonDialog = false }
        )
    }
    if (showAddPointsSheet && !state.isFinished) {
        AddPointsSheet(
            maxPoints = state.maxPointsAtOnce,
            pointsToWin = state.pointsToWin,
            onAdd = { points ->
                actions.onDismissAddPoints()
                actions.onAddPoints(points)
            },
            onDismiss = actions.onDismissAddPoints
        )
    }
    if (state.showOpeningBreakFoulDialog) {
        OpeningBreakFoulSheet(onChoice = actions.onResolveOpeningFoul, onDismiss = actions.onDismissOpeningFoul)
    }
    if (state.isFinished) {
        GameResultSheet(state = state, onDone = actions.onDone, onDiscard = actions.onDiscard, onUndo = actions.onUndo)
    }
}

@Composable
private fun CompactGameLayout(state: GameUiState, actions: GameActions, scoreboardMaxHeight: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Scoreboard(
                state = state,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp)
                    .heightIn(max = scoreboardMaxHeight)
            )
            StatusRow(state, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            HistoryHeader(Modifier.padding(horizontal = 16.dp))
            BreakHistory(
                breaks = state.breaks,
                headToHeadLeftPlayerId = state.players.first().id.takeIf { state.isHeadToHead },
                showPlayerNames = !state.isPractice,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = ToolbarHeight + 32.dp
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
        GameToolbar(
            state = state,
            actions = actions,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun WideGameLayout(state: GameUiState, actions: GameActions) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(Modifier.weight(1f)) {
                Scoreboard(state = state, modifier = Modifier.weight(1f, fill = false))
                StatusRow(state, Modifier.padding(vertical = 12.dp))
            }
            GameToolbar(
                state = state,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column {
                HistoryHeader(Modifier.padding(start = 16.dp, top = 16.dp))
                BreakHistory(
                    breaks = state.breaks,
                    headToHeadLeftPlayerId = state.players.first().id.takeIf { state.isHeadToHead },
                    showPlayerNames = !state.isPractice,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun Scoreboard(state: GameUiState, modifier: Modifier = Modifier) {
    val winnerId = state.winner?.id
    val target = state.targetScore
    when {
        target == null -> PracticeScoreboard(state.players.first(), modifier)
        state.isHeadToHead -> HeadToHeadScoreboard(state.players, state.atTableIndex, target, winnerId, modifier)
        else -> PlayerListScoreboard(state.players, state.atTableIndex, target, winnerId, modifier)
    }
}

@Composable
private fun GameToolbar(state: GameUiState, actions: GameActions, modifier: Modifier = Modifier) {
    ActionToolbar(
        enabled = !state.isFinished,
        isOpeningBreak = state.isOpeningBreak && !state.isFinished,
        onAddPoint = actions.onAddPoint,
        onOpenAddPoints = actions.onShowAddPoints,
        onBreakShot = actions.onBreakShot,
        onSafe = actions.onSafe,
        onMiss = actions.onMiss,
        onFoul = actions.onFoul,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusRow(state: GameUiState, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoPill(MaterialSymbols.ATR, stringResource(R.string.game_rack, state.table.rackNumber))
        InfoPill(
            MaterialSymbols.CIRCLE,
            pluralStringResource(R.plurals.game_balls_on_table, state.table.ballsOnTable, state.table.ballsOnTable)
        )
        if (state.table.isOpeningBreak && !state.isFinished) {
            InfoPill(
                symbol = MaterialSymbols.FLAG,
                text = stringResource(R.string.game_opening_break),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun InfoPill(
    symbol: String,
    text: String,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedRoundedSymbol(symbol, size = 16.dp, tint = content)
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun HistoryHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.history_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(
    targetScore: Int?,
    canUndo: Boolean,
    canAbandon: Boolean,
    canFinishPractice: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onAbandon: () -> Unit,
    onFinishPractice: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val backDescription = stringResource(R.string.action_back)
    val undoDescription = stringResource(R.string.game_undo)
    val moreDescription = stringResource(R.string.action_more)

    TopAppBar(
        title = {
            Text(
                if (targetScore != null) stringResource(R.string.game_title, targetScore)
                else stringResource(R.string.practice_title)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backDescription }) {
                FilledRoundedSymbol(MaterialSymbols.ARROW_BACK)
            }
        },
        actions = {
            if (canFinishPractice) {
                FilledTonalButton(onClick = onFinishPractice, modifier = Modifier.padding(end = 4.dp)) {
                    Text(stringResource(R.string.practice_finish))
                }
            }
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.semantics { contentDescription = undoDescription }
            ) {
                FilledRoundedSymbol(
                    MaterialSymbols.UNDO,
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            if (canAbandon) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics { contentDescription = moreDescription }
                    ) {
                        FilledRoundedSymbol(MaterialSymbols.MORE_VERT)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.game_abandon)) },
                            leadingIcon = { OutlinedRoundedSymbol(MaterialSymbols.CLOSE) },
                            onClick = {
                                menuExpanded = false
                                onAbandon()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AbandonDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { OutlinedRoundedSymbol(MaterialSymbols.WARNING) },
        title = { Text(stringResource(R.string.game_abandon_title)) },
        text = { Text(stringResource(R.string.game_abandon_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.game_abandon_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(name = "Head to head", showSystemUi = true)
@Composable
private fun HeadToHeadPreview() {
    StraightScorerTheme { GameContent(SampleGames.headToHead(), GameActions(), showAddPointsSheet = false) }
}

@Preview(name = "Four players", showSystemUi = true)
@Composable
private fun MultiPlayerPreview() {
    StraightScorerTheme(darkTheme = true) {
        GameContent(SampleGames.multiPlayer(), GameActions(), showAddPointsSheet = false)
    }
}

@Preview(name = "Practice", showSystemUi = true)
@Composable
private fun PracticePreview() {
    StraightScorerTheme { GameContent(SampleGames.practice(), GameActions(), showAddPointsSheet = false) }
}
