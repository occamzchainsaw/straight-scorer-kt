package com.occamzchainsaw.straightscorer.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.common.StatsRowData
import com.occamzchainsaw.straightscorer.ui.common.StatsTable
import com.occamzchainsaw.straightscorer.ui.common.formatDuration
import com.occamzchainsaw.straightscorer.ui.theme.OutcomeColor
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import com.occamzchainsaw.straightscorer.ui.theme.tabularNumbers
import com.occamzchainsaw.straightscorer.viewmodels.GameUiState
import com.occamzchainsaw.straightscorer.viewmodels.OpeningBreakFoul
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import kotlin.time.Duration.Companion.milliseconds

/** Quick picks for multi-ball shots and a field for anything else. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPointsSheet(
    maxPoints: Int,
    /** Shown as a hint in a match; null in practice. */
    pointsToWin: Int?,
    onAdd: (Int) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var custom by rememberSaveable { mutableStateOf("") }
    val customValue = custom.toIntOrNull()
    val customValid = customValue != null && customValue in 1..maxPoints

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SheetHeader(
                title = stringResource(R.string.add_points_title),
                subtitle = pointsToWin?.let { stringResource(R.string.add_points_subtitle, it) }
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 5
            ) {
                (2..14).forEach { points ->
                    FilledTonalButton(
                        onClick = { onAdd(points) },
                        enabled = points <= maxPoints,
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "+$points",
                            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // fill the last row so the buttons keep the same width
                repeat(2) { Spacer(Modifier.weight(1f)) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { text -> if (text.length <= 4 && text.all(Char::isDigit)) custom = text },
                    label = { Text(stringResource(R.string.add_points_custom)) },
                    isError = custom.isNotEmpty() && !customValid,
                    supportingText = if (custom.isNotEmpty() && !customValid) {
                        { Text(stringResource(R.string.add_points_invalid, maxPoints)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (customValid) onAdd(customValue!!) }),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onAdd(customValue!!) },
                    enabled = customValid,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(if (customValid) stringResource(R.string.add_points_add, customValue!!) else stringResource(R.string.add_points_add_empty))
                }
            }
        }
    }
}

/** Rules 4 and 11: on the opening break the referee decides what kind of foul it was. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningBreakFoulSheet(
    onChoice: (OpeningBreakFoul) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val outcomes = MaterialTheme.outcomeColors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 24.dp)) {
            SheetHeader(
                title = stringResource(R.string.opening_foul_title),
                subtitle = stringResource(R.string.opening_foul_subtitle),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            FoulChoice(
                title = stringResource(R.string.opening_foul_accept),
                text = stringResource(R.string.opening_foul_accept_text),
                penalty = "−2",
                outcome = outcomes.breakingFoul,
                onClick = { onChoice(OpeningBreakFoul.BreakingAcceptTable) }
            )
            FoulChoice(
                title = stringResource(R.string.opening_foul_rebreak),
                text = stringResource(R.string.opening_foul_rebreak_text),
                penalty = "−2",
                outcome = outcomes.breakingFoul,
                onClick = { onChoice(OpeningBreakFoul.BreakingReBreak) }
            )
            Text(
                text = stringResource(R.string.opening_foul_standard_header),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
            )
            FoulChoice(
                title = stringResource(R.string.opening_foul_standard),
                text = stringResource(R.string.opening_foul_standard_text),
                penalty = "−1",
                outcome = outcomes.foul,
                onClick = { onChoice(OpeningBreakFoul.Standard) }
            )
        }
    }
}

@Composable
private fun FoulChoice(title: String, text: String, penalty: String, outcome: OutcomeColor, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(text) },
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(outcome.container, CircleShape)
            ) {
                Text(
                    text = penalty,
                    color = outcome.onContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    )
}

/**
 * Results once the target is reached. Can't be swiped away - the game stays open until
 * the user picks Done, Discard or Undo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameResultSheet(
    state: GameUiState,
    onDone: () -> Unit,
    onDiscard: () -> Unit,
    onUndo: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
) {
    val winner = state.winner
    if (winner == null && !state.isPractice) return
    val win = MaterialTheme.outcomeColors.win
    val game = state.game
    val duration = formatDuration((game.finishedAt ?: game.startedAt) - game.startedAt)

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(win.container, CircleShape)
            ) {
                FilledRoundedSymbol(
                    if (winner != null) MaterialSymbols.TROPHY else MaterialSymbols.SPORTS_SCORE,
                    size = 40.dp,
                    tint = win.onContainer
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (winner != null) stringResource(R.string.result_wins, winner.name)
                    else stringResource(R.string.practice_finished),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = game.targetScore?.let { target ->
                        stringResource(R.string.result_summary, target, game.table.rackNumber, duration)
                    } ?: stringResource(R.string.practice_summary, game.table.rackNumber, duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatsTable(
                rows = state.players.sortedByDescending { it.score }.map { player ->
                    StatsRowData(
                        name = player.name,
                        score = player.score,
                        highestBreak = player.highestBreak,
                        averageBreak = player.averageBreak,
                        fouls = player.totalFouls,
                        highlighted = player.id == winner?.id
                    )
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledRoundedSymbol(
                    MaterialSymbols.CHECK_CIRCLE,
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.result_saved),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.canUndo) {
                    TextButton(onClick = onUndo) { Text(stringResource(R.string.result_undo)) }
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onDiscard,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.result_discard))
                }
                Button(onClick = onDone) { Text(stringResource(R.string.result_done)) }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
