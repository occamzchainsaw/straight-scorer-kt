package com.occamzchainsaw.straightscorer.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.theme.ScorerMotion
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import com.occamzchainsaw.straightscorer.ui.theme.tabularNumbers
import com.occamzchainsaw.straightscorer.ui.theme.cardContainer

/** Two cards side by side, mirrored: the left player reads from the left edge, the right one from the right edge. */
@Composable
fun HeadToHeadScoreboard(
    players: List<Player>,
    atTableIndex: Int,
    targetScore: Int,
    winnerId: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.take(2).forEachIndexed { index, player ->
            HeadToHeadPlayerCard(
                player = player,
                isAtTable = winnerId == null && index == atTableIndex,
                isWinner = player.id == winnerId,
                targetScore = targetScore,
                mirrored = index == 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun HeadToHeadPlayerCard(
    player: Player,
    isAtTable: Boolean,
    isWinner: Boolean,
    targetScore: Int,
    mirrored: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = playerCardColors(isAtTable, isWinner)
    val horizontal = if (mirrored) Alignment.End else Alignment.Start

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        contentColor = colors.content
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = horizontal
        ) {
            MirroredRow(mirrored = mirrored) {
                AtTableDot(visible = isAtTable, color = colors.accent)
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            AnimatedScore(
                score = player.score,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                alignment = if (mirrored) Alignment.CenterEnd else Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth()
            )
            ScoreProgress(
                score = player.score,
                targetScore = targetScore,
                color = colors.accent,
                mirrored = mirrored
            )
            Spacer(Modifier.height(10.dp))
            MirroredRow(mirrored = mirrored) {
                Text(
                    text = stringResource(R.string.game_break, player.currentBreak),
                    style = MaterialTheme.typography.labelLarge.tabularNumbers(),
                    color = colors.content.copy(alpha = if (isAtTable) 1f else 0.7f)
                )
                FoulDots(count = player.consecutiveFouls)
            }
        }
    }
}

/** A row whose children are laid out right-to-left when [mirrored], keeping the card symmetric. */
@Composable
private fun MirroredRow(mirrored: Boolean, content: @Composable RowScope.() -> Unit) {
    val direction = if (mirrored) LayoutDirection.Rtl else LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

/** Single-player practice: the score plus the stats the session is about. */
@Composable
fun PracticeScoreboard(player: Player, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedScore(
                    score = player.score,
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    alignment = Alignment.CenterStart
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.game_break, player.currentBreak),
                        style = MaterialTheme.typography.labelLarge.tabularNumbers()
                    )
                    FoulDots(count = player.consecutiveFouls)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PracticeStat(stringResource(R.string.practice_highest), player.highestBreak.toString())
                PracticeStat(
                    stringResource(R.string.practice_average),
                    String.format(locale, "%.1f", player.averageBreak)
                )
            }
        }
    }
}

@Composable
private fun PracticeStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.tabularNumbers(),
            fontWeight = FontWeight.Bold
        )
    }
}

/** Turn order list for three or more players; keeps the player at the table scrolled into view. */
@Composable
fun PlayerListScoreboard(
    players: List<Player>,
    atTableIndex: Int,
    targetScore: Int,
    winnerId: Int?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(atTableIndex) { listState.animateScrollToItem(atTableIndex) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.cardContainer
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(players, key = { _, player -> player.id }) { index, player ->
                PlayerListRow(
                    player = player,
                    turnNumber = index + 1,
                    isAtTable = winnerId == null && index == atTableIndex,
                    isWinner = player.id == winnerId,
                    targetScore = targetScore
                )
            }
        }
    }
}

@Composable
private fun PlayerListRow(
    player: Player,
    turnNumber: Int,
    isAtTable: Boolean,
    isWinner: Boolean,
    targetScore: Int
) {
    val colors = playerCardColors(isAtTable, isWinner, idle = Color.Transparent)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.content
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isAtTable || isWinner) colors.accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape
                    )
            ) {
                Text(
                    text = turnNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isAtTable || isWinner) MaterialTheme.colorScheme.surface else colors.content
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isAtTable) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isAtTable) {
                    Text(
                        text = stringResource(R.string.game_break, player.currentBreak),
                        style = MaterialTheme.typography.labelMedium.tabularNumbers()
                    )
                }
                ScoreProgress(
                    score = player.score,
                    targetScore = targetScore,
                    color = colors.accent,
                    mirrored = false,
                    height = 3.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            FoulDots(count = player.consecutiveFouls)
            AnimatedScore(
                score = player.score,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                alignment = Alignment.CenterEnd,
                modifier = Modifier.width(72.dp)
            )
        }
    }
}

private data class PlayerCardColors(val container: Color, val content: Color, val accent: Color)

@Composable
private fun playerCardColors(
    isAtTable: Boolean,
    isWinner: Boolean,
    idle: Color = MaterialTheme.colorScheme.surfaceContainerHigh
): PlayerCardColors {
    val scheme = MaterialTheme.colorScheme
    val win = MaterialTheme.outcomeColors.win
    val target = when {
        isWinner -> PlayerCardColors(win.container, win.onContainer, win.color)
        isAtTable -> PlayerCardColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary)
        else -> PlayerCardColors(idle, scheme.onSurface, scheme.outline)
    }
    val spec = tween<Color>(ScorerMotion.MEDIUM, easing = ScorerMotion.Emphasized)
    val container by animateColorAsState(target.container, spec, label = "container")
    val content by animateColorAsState(target.content, spec, label = "content")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    return PlayerCardColors(container, content, accent)
}

/** Score that rolls up when it grows and down when a foul takes points away. */
@Composable
fun AnimatedScore(
    score: Int,
    style: TextStyle,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = score,
        modifier = modifier,
        contentAlignment = alignment,
        transitionSpec = {
            val up = targetState > initialState
            val enter = slideInVertically(tween(ScorerMotion.MEDIUM, easing = ScorerMotion.EmphasizedDecelerate)) {
                if (up) it else -it
            } + fadeIn(tween(ScorerMotion.MEDIUM))
            val exit = slideOutVertically(tween(ScorerMotion.SHORT, easing = ScorerMotion.EmphasizedAccelerate)) {
                if (up) -it else it
            } + fadeOut(tween(ScorerMotion.SHORT))
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        label = "score"
    ) { value ->
        Text(
            text = formatScore(value),
            style = style.tabularNumbers(),
            textAlign = if (alignment == Alignment.CenterEnd) TextAlign.End else TextAlign.Start,
            maxLines = 1
        )
    }
}

@Composable
private fun ScoreProgress(
    score: Int,
    targetScore: Int,
    color: Color,
    mirrored: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    val fraction = (score.toFloat() / targetScore).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(ScorerMotion.LONG, easing = ScorerMotion.Emphasized),
        label = "progress"
    )
    val percent = (fraction * 100).toInt()
    val description = stringResource(R.string.game_progress, percent)
    LinearProgressIndicator(
        progress = { progress },
        color = color,
        trackColor = color.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { if (mirrored) scaleX = -1f }
            .semantics { contentDescription = description }
    )
}

/** Up to two dots for consecutive fouls - the third one triggers the penalty and resets the count. */
@Composable
fun FoulDots(count: Int, modifier: Modifier = Modifier) {
    val foul = MaterialTheme.outcomeColors.foul
    val description = pluralStringResource(R.plurals.game_consecutive_fouls, count, count)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(2) { index ->
            val filled = index < count
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        if (filled) foul.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun AtTableDot(visible: Boolean, color: Color) {
    val description = stringResource(R.string.game_at_table)
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(tween(ScorerMotion.MEDIUM, easing = ScorerMotion.EmphasizedDecelerate)) + fadeIn(),
        exit = shrinkHorizontally(tween(ScorerMotion.SHORT, easing = ScorerMotion.EmphasizedAccelerate)) + fadeOut()
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape)
                .semantics { contentDescription = description }
        )
    }
}

/** Uses a real minus sign, which lines up with the digits. */
fun formatScore(score: Int): String = if (score < 0) "\u2212${-score}" else score.toString()
