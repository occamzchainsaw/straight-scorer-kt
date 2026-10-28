package com.occamzchainsaw.straightscorer.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.core.models.MatchResult
import com.occamzchainsaw.core.models.PlayerMatchSummary
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.common.SwipeToRemove
import com.occamzchainsaw.straightscorer.ui.common.currentLocale
import com.occamzchainsaw.straightscorer.ui.common.formatDateTime
import com.occamzchainsaw.straightscorer.ui.common.formatDuration
import com.occamzchainsaw.straightscorer.ui.common.formatMonth
import com.occamzchainsaw.straightscorer.ui.game.formatScore
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import com.occamzchainsaw.straightscorer.ui.theme.tabularNumbers
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.HistoryEvent
import com.occamzchainsaw.straightscorer.viewmodels.HistoryFilter
import com.occamzchainsaw.straightscorer.viewmodels.HistoryUiState
import com.occamzchainsaw.straightscorer.viewmodels.HistoryViewModel
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol
import com.occamzchainsaw.straightscorer.ui.theme.cardContainer

@Immutable
data class HistoryActions(
    val onOpenMatch: (String) -> Unit = {},
    val onDeleteMatch: (String) -> Unit = {},
    val onClearHistory: () -> Unit = {},
    val onFilterChange: (HistoryFilter) -> Unit = {}
)

/**
 * @param pendingDeleteId a match deleted from its detail screen; deleted here so the snackbar can offer undo.
 */
@Composable
fun HistoryScreen(
    onOpenMatch: (String) -> Unit,
    pendingDeleteId: String?,
    onPendingDeleteHandled: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    val undoLabel = stringResource(R.string.action_undo)
    val pendingDeleteHandled by rememberUpdatedState(onPendingDeleteHandled)

    LaunchedEffect(pendingDeleteId) {
        if (pendingDeleteId != null) {
            viewModel.deleteMatch(pendingDeleteId)
            pendingDeleteHandled()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.MatchesDeleted -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getQuantityString(R.plurals.history_deleted, event.count, event.count),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
            }
        }
    }

    val actions = remember(viewModel, onOpenMatch) {
        HistoryActions(
            onOpenMatch = onOpenMatch,
            onDeleteMatch = viewModel::deleteMatch,
            onClearHistory = viewModel::clearHistory,
            onFilterChange = viewModel::setFilter
        )
    }
    HistoryContent(state = uiState, actions = actions, snackbarHostState = snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    state: HistoryUiState,
    actions: HistoryActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showClearDialog by remember { mutableStateOf(false) }
    val locale = currentLocale()
    val sections = remember(state.visibleMatches, locale) {
        state.visibleMatches.groupBy { formatMonth(it.finishedAt, locale) }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HistoryTopBar(
                canClear = state.canClear,
                onClear = { showClearDialog = true },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.matches.isEmpty() -> EmptyHistory(
                title = stringResource(R.string.history_empty_title),
                text = stringResource(R.string.history_empty_text),
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "filters") {
                    FilterRow(state.filter, actions.onFilterChange)
                }
                if (sections.isEmpty()) {
                    item(key = "empty-filter") {
                        Text(
                            text = stringResource(R.string.history_empty_filtered),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }
                sections.forEach { (month, matches) ->
                    stickyHeader(key = "month-$month") {
                        MonthHeader(month)
                    }
                    items(matches, key = { it.id }) { match ->
                        SwipeToRemove(
                            enabled = true,
                            onRemove = { actions.onDeleteMatch(match.id) },
                            modifier = Modifier.animateItem()
                        ) {
                            MatchCard(match = match, onClick = { actions.onOpenMatch(match.id) })
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { OutlinedRoundedSymbol(MaterialSymbols.DELETE_SWEEP) },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    actions.onClearHistory()
                }) { Text(stringResource(R.string.history_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    canClear: Boolean,
    onClear: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val moreDescription = stringResource(R.string.action_more)
    TopAppBar(
        title = { Text(stringResource(R.string.nav_history)) },
        scrollBehavior = scrollBehavior,
        actions = {
            if (canClear) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics { contentDescription = moreDescription }
                    ) {
                        FilledRoundedSymbol(MaterialSymbols.MORE_VERT)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_clear)) },
                            leadingIcon = { OutlinedRoundedSymbol(MaterialSymbols.DELETE_SWEEP) },
                            onClick = {
                                menuExpanded = false
                                onClear()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun FilterRow(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        HistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        stringResource(
                            when (filter) {
                                HistoryFilter.All -> R.string.history_filter_all
                                HistoryFilter.Matches -> R.string.history_filter_matches
                                HistoryFilter.Practice -> R.string.history_filter_practice
                            }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun MonthHeader(month: String) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = month,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
        )
    }
}

@Composable
fun MatchCard(match: MatchResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.cardContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDateTime(match.finishedAt, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TypePill(match)
            }
            when {
                match.isPractice -> PracticeSummary(match.players.first())
                match.players.size == 2 -> HeadToHeadSummary(match.players[0], match.players[1])
                else -> MultiPlayerSummary(match)
            }
            Text(
                text = listOf(
                    stringResource(R.string.history_high_break, match.players.maxOf { it.highestBreak }),
                    pluralStringResource(R.plurals.history_racks, match.racks, match.racks),
                    formatDuration(match.finishedAt - match.startedAt)
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TypePill(match: MatchResult) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = if (match.isPractice) {
        scheme.tertiaryContainer to scheme.onTertiaryContainer
    } else {
        scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Text(
            text = match.targetScore?.let { stringResource(R.string.history_race_to, it) }
                ?: stringResource(R.string.history_practice),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** Mirrored like the in-game scoreboard: left player on the left, right player on the right. */
@Composable
private fun HeadToHeadSummary(left: PlayerMatchSummary, right: PlayerMatchSummary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SummarySide(left, mirrored = false, modifier = Modifier.weight(1f))
        Text(
            "–",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        SummarySide(right, mirrored = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummarySide(player: PlayerMatchSummary, mirrored: Boolean, modifier: Modifier = Modifier) {
    val win = MaterialTheme.outcomeColors.win
    val name: @Composable RowScope.() -> Unit = {
        Text(
            text = player.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (player.isWinner) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
    val score: @Composable () -> Unit = {
        Text(
            text = formatScore(player.finalScore),
            style = MaterialTheme.typography.headlineSmall.tabularNumbers(),
            fontWeight = FontWeight.Bold,
            color = if (player.isWinner) win.color else MaterialTheme.colorScheme.onSurface
        )
    }
    val trophy: @Composable () -> Unit = {
        if (player.isWinner) FilledRoundedSymbol(MaterialSymbols.TROPHY, size = 18.dp, tint = win.color)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, if (mirrored) Alignment.End else Alignment.Start)
    ) {
        if (mirrored) {
            score(); name(); trophy()
        } else {
            trophy(); name(); score()
        }
    }
}

@Composable
private fun MultiPlayerSummary(match: MatchResult) {
    val win = MaterialTheme.outcomeColors.win
    val winner = match.winner ?: match.players.maxBy { it.finalScore }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledRoundedSymbol(MaterialSymbols.TROPHY, size = 18.dp, tint = win.color)
        Text(
            text = stringResource(R.string.history_winner, winner.name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            text = formatScore(winner.finalScore),
            style = MaterialTheme.typography.headlineSmall.tabularNumbers(),
            fontWeight = FontWeight.Bold,
            color = win.color
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = pluralStringResource(R.plurals.history_players, match.players.size, match.players.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PracticeSummary(player: PlayerMatchSummary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = player.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.history_practice_points, formatScore(player.finalScore)),
            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyHistory(title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        ) {
            OutlinedRoundedSymbol(MaterialSymbols.HISTORY, size = 48.dp, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(0.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
