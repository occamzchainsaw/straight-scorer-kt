package com.occamzchainsaw.straightscorer.ui.game

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.BreakEndAction
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.theme.OutcomeColor
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import com.occamzchainsaw.straightscorer.ui.theme.tabularNumbers

private val RowHeight = 48.dp
private val RailWidth = 28.dp
private val NodeRadius = 6.dp
private val LineWidth = 2.dp

private sealed interface HistoryItem {
    val key: String

    data class Entry(val entry: Break, val index: Int, val isNewest: Boolean, val isOldest: Boolean) : HistoryItem {
        override val key get() = "break-$index"
    }

    data class RackDivider(val rackNumber: Int) : HistoryItem {
        override val key get() = "rack-$rackNumber"
    }
}

/** Newest first, with a divider wherever a new rack started. */
private fun historyItems(breaks: List<Break>): List<HistoryItem> = buildList {
    for (index in breaks.indices.reversed()) {
        val entry = breaks[index]
        add(HistoryItem.Entry(entry, index, isNewest = index == breaks.lastIndex, isOldest = index == 0))
        val older = breaks.getOrNull(index - 1)
        if (older != null && older.rackNumber != entry.rackNumber) add(HistoryItem.RackDivider(entry.rackNumber))
    }
}

/**
 * Timeline of every visit. Head to head: a centre rail with the left player's entries on the left
 * and the right player's on the right. Otherwise a single rail with names.
 */
@Composable
fun BreakHistory(
    breaks: List<Break>,
    headToHeadLeftPlayerId: Int?,
    modifier: Modifier = Modifier,
    showPlayerNames: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState()
) {
    // always bring the newest entry into view
    LaunchedEffect(breaks.size) {
        listState.animateScrollToItem(0)
    }

    if (breaks.isEmpty()) {
        Box(modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.TopCenter) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        breakHistoryItems(breaks, headToHeadLeftPlayerId, showPlayerNames)
    }
}

/** The timeline as list items, for screens that put it inside their own LazyColumn. */
fun LazyListScope.breakHistoryItems(
    breaks: List<Break>,
    headToHeadLeftPlayerId: Int?,
    showPlayerNames: Boolean = true
) {
    items(historyItems(breaks), key = { it.key }) { item ->
        when (item) {
            is HistoryItem.RackDivider -> RackDivider(item.rackNumber, Modifier.animateItem())
            is HistoryItem.Entry -> {
                val rowModifier = Modifier.animateItem()
                if (headToHeadLeftPlayerId != null) {
                    HeadToHeadHistoryRow(item, isLeft = item.entry.playerId == headToHeadLeftPlayerId, modifier = rowModifier)
                } else {
                    SingleLaneHistoryRow(item, showPlayerName = showPlayerNames, modifier = rowModifier)
                }
            }
        }
    }
}

@Composable
private fun HeadToHeadHistoryRow(item: HistoryItem.Entry, isLeft: Boolean, modifier: Modifier = Modifier) {
    val outcome = MaterialTheme.outcomeColors.of(item.entry.endAction)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (isLeft) BreakPill(item.entry, outcome)
        }
        TimelineRail(
            outcome = outcome,
            drawAbove = !item.isNewest,
            drawBelow = !item.isOldest,
            connectLeft = isLeft,
            connectRight = !isLeft,
            modifier = Modifier.width(RailWidth + 16.dp)
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (!isLeft) BreakPill(item.entry, outcome)
        }
    }
}

@Composable
private fun SingleLaneHistoryRow(item: HistoryItem.Entry, showPlayerName: Boolean, modifier: Modifier = Modifier) {
    val outcome = MaterialTheme.outcomeColors.of(item.entry.endAction)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineRail(
            outcome = outcome,
            drawAbove = !item.isNewest,
            drawBelow = !item.isOldest,
            connectLeft = false,
            connectRight = true,
            modifier = Modifier.width(RailWidth + 8.dp)
        )
        if (showPlayerName) {
            Text(
                text = item.entry.playerName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f, fill = false)
            )
        }
        BreakPill(item.entry, outcome)
    }
}

@Composable
private fun TimelineRail(
    outcome: OutcomeColor,
    drawAbove: Boolean,
    drawBelow: Boolean,
    connectLeft: Boolean,
    connectRight: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val ringColor = MaterialTheme.colorScheme.surface
    Canvas(modifier.fillMaxHeight()) {
        val center = Offset(size.width / 2, size.height / 2)
        val stroke = LineWidth.toPx()
        if (drawAbove) drawLine(lineColor, Offset(center.x, 0f), center, stroke)
        if (drawBelow) drawLine(lineColor, center, Offset(center.x, size.height), stroke)
        if (connectLeft) drawLine(outcome.color.copy(alpha = 0.5f), Offset(0f, center.y), center, stroke)
        if (connectRight) drawLine(outcome.color.copy(alpha = 0.5f), center, Offset(size.width, center.y), stroke)
        drawCircle(ringColor, NodeRadius.toPx() + stroke, center)
        drawCircle(outcome.color, NodeRadius.toPx(), center)
    }
}

@Composable
private fun BreakPill(entry: Break, outcome: OutcomeColor) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = outcome.container,
        contentColor = outcome.onContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formatPoints(entry.netPoints),
                style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(entry.endAction.label),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun RackDivider(rackNumber: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.game_rack, rackNumber),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

fun formatPoints(points: Int): String = when {
    points > 0 -> "+$points"
    points < 0 -> "−${-points}"
    else -> "0"
}

@get:StringRes
val BreakEndAction.label: Int
    get() = when (this) {
        BreakEndAction.Win -> R.string.outcome_win
        BreakEndAction.Safe -> R.string.outcome_safe
        BreakEndAction.Miss -> R.string.outcome_miss
        BreakEndAction.Foul -> R.string.outcome_foul
        BreakEndAction.BreakingFoul -> R.string.outcome_breaking_foul
        BreakEndAction.ThirdFoul -> R.string.outcome_third_foul
        BreakEndAction.BreakShot -> R.string.outcome_break_shot
        BreakEndAction.Finish -> R.string.outcome_finish
    }
