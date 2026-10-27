package com.occamzchainsaw.straightscorer.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.common.StatsRowData
import com.occamzchainsaw.straightscorer.ui.common.StatsTable
import com.occamzchainsaw.straightscorer.ui.common.currentLocale
import com.occamzchainsaw.straightscorer.ui.common.formatDateTime
import com.occamzchainsaw.straightscorer.ui.common.formatDuration
import com.occamzchainsaw.straightscorer.ui.game.breakHistoryItems
import com.occamzchainsaw.straightscorer.ui.game.formatScore
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.MatchDetailUiState
import com.occamzchainsaw.straightscorer.viewmodels.MatchDetailViewModel
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

@Composable
fun MatchDetailScreen(
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    viewModel: MatchDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    MatchDetailContent(state = uiState, onBack = onBack, onDelete = { onDelete(viewModel.matchId) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailContent(
    state: MatchDetailUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val match = (state as? MatchDetailUiState.Loaded)?.match
    val backDescription = stringResource(R.string.action_back)
    val deleteDescription = stringResource(R.string.action_delete)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (match?.isPractice == true) R.string.detail_practice_title else R.string.detail_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backDescription }) {
                        FilledRoundedSymbol(MaterialSymbols.ARROW_BACK)
                    }
                },
                actions = {
                    if (match != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = deleteDescription }) {
                            OutlinedRoundedSymbol(MaterialSymbols.DELETE)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            MatchDetailUiState.Loading -> Centered(Modifier.padding(padding)) { CircularProgressIndicator() }
            MatchDetailUiState.NotFound -> Centered(Modifier.padding(padding)) {
                Text(stringResource(R.string.detail_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is MatchDetailUiState.Loaded -> MatchDetailBody(state.match, PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ))
        }
    }
}

@Composable
private fun MatchDetailBody(match: MatchResult, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item(key = "header") { MatchHeader(match) }
        item(key = "players-title") { SectionTitle(stringResource(R.string.detail_players)) }
        item(key = "players") {
            StatsTable(
                rows = match.players.sortedByDescending { it.finalScore }.map { player ->
                    StatsRowData(
                        name = player.name,
                        score = player.finalScore,
                        highestBreak = player.highestBreak,
                        averageBreak = player.averageBreak,
                        fouls = player.totalFouls,
                        highlighted = player.isWinner
                    )
                }
            )
        }
        if (match.breaks.isNotEmpty()) {
            item(key = "breaks-title") { SectionTitle(stringResource(R.string.detail_break_history)) }
            // saved player ids are their positions in the original line-up
            breakHistoryItems(
                breaks = match.breaks,
                headToHeadLeftPlayerId = 0.takeIf { match.players.size == 2 },
                showPlayerNames = !match.isPractice
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchHeader(match: MatchResult) {
    val win = MaterialTheme.outcomeColors.win
    val scheme = MaterialTheme.colorScheme
    val winner = match.winner
    val locale = currentLocale()

    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (winner != null) win.container else scheme.tertiaryContainer,
                            CircleShape
                        )
                ) {
                    FilledRoundedSymbol(
                        if (winner != null) MaterialSymbols.TROPHY else MaterialSymbols.FITNESS_CENTER,
                        size = 26.dp,
                        tint = if (winner != null) win.onContainer else scheme.onTertiaryContainer
                    )
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(
                        text = when {
                            winner != null -> stringResource(R.string.detail_wins, winner.name)
                            else -> match.players.first().name
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDateTime(match.finishedAt, locale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            if (match.players.size == 2) {
                Text(
                    text = "${match.players[0].name} ${formatScore(match.players[0].finalScore)} – " +
                        "${formatScore(match.players[1].finalScore)} ${match.players[1].name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailPill(
                    if (match.isPractice) MaterialSymbols.FITNESS_CENTER else MaterialSymbols.TROPHY,
                    match.targetScore?.let { stringResource(R.string.history_race_to, it) }
                        ?: stringResource(R.string.history_practice)
                )
                DetailPill(MaterialSymbols.ATR, pluralStringResource(R.plurals.history_racks, match.racks, match.racks))
                DetailPill(MaterialSymbols.TIMER, formatDuration(match.finishedAt - match.startedAt))
            }
        }
    }
}

@Composable
private fun DetailPill(symbol: String, text: String) {
    val content = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedRoundedSymbol(symbol, size = 16.dp, tint = content)
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
