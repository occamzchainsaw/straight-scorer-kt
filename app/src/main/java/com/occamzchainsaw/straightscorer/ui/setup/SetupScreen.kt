package com.occamzchainsaw.straightscorer.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.common.SwipeToRemove
import com.occamzchainsaw.straightscorer.dtos.PlayerSetupItem
import com.occamzchainsaw.straightscorer.ui.game.formatScore
import com.occamzchainsaw.straightscorer.ui.theme.StraightScorerTheme
import com.occamzchainsaw.straightscorer.ui.theme.outcomeColors
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.PlayerSetupErrors
import com.occamzchainsaw.straightscorer.viewmodels.SetupEvent
import com.occamzchainsaw.straightscorer.viewmodels.SetupUiState
import com.occamzchainsaw.straightscorer.viewmodels.SetupViewModel
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Immutable
data class SetupActions(
    val onTargetScoreChange: (String) -> Unit = {},
    val onPlayerChange: (PlayerSetupItem) -> Unit = {},
    val onSetStartingPlayer: (String) -> Unit = {},
    val onRemovePlayer: (PlayerSetupItem) -> Unit = {},
    val onMovePlayer: (id: String, targetId: String) -> Unit = { _, _ -> },
    val onAddPlayer: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onResume: () -> Unit = {}
)

@Composable
fun SetupScreen(
    onNavigateToGame: () -> Unit,
    viewModel: SetupViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigateToGame by rememberUpdatedState(onNavigateToGame)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showReplaceDialog by remember { mutableStateOf(false) }

    val removedMessage = stringResource(R.string.setup_player_removed, "%s")
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SetupEvent.NavigateToGame -> navigateToGame()
                SetupEvent.ConfirmReplaceGame -> showReplaceDialog = true
            }
        }
    }

    val actions = remember(viewModel) {
        SetupActions(
            onTargetScoreChange = viewModel::updateTargetScore,
            onPlayerChange = viewModel::updatePlayer,
            onSetStartingPlayer = viewModel::setStartingPlayer,
            onRemovePlayer = { player ->
                viewModel.removePlayer(player.id)
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = removedMessage.format(player.name.ifBlank { "Player" }),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemovePlayer()
                }
            },
            onMovePlayer = viewModel::movePlayer,
            onAddPlayer = viewModel::addPlayer,
            onStart = viewModel::startGame,
            onResume = viewModel::resumeGame
        )
    }

    SetupContent(state = uiState, actions = actions, snackbarHostState = snackbarHostState)

    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            icon = { OutlinedRoundedSymbol(MaterialSymbols.WARNING) },
            title = { Text(stringResource(R.string.setup_replace_title)) },
            text = { Text(stringResource(R.string.setup_replace_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceDialog = false
                    viewModel.confirmNewGame()
                }) { Text(stringResource(R.string.setup_replace_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupContent(
    state: SetupUiState,
    actions: SetupActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        actions.onMovePlayer(fromKey, toKey)
    }
    val playerErrors = state.playerErrors

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = actions.onStart,
                icon = { FilledRoundedSymbol(MaterialSymbols.PLAY_ARROW) },
                text = {
                    Text(
                        stringResource(if (state.isPractice) R.string.setup_start_practice else R.string.setup_start_game)
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.activeGame?.let { game ->
                item(key = "resume") {
                    ResumeCard(game = game, onResume = actions.onResume, modifier = Modifier.animateItem())
                }
            }
            item(key = "target") {
                if (state.isPractice) {
                    PracticeCard(Modifier.animateItem())
                } else {
                    TargetScoreCard(state, actions.onTargetScoreChange, Modifier.animateItem())
                }
            }
            item(key = "players-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.setup_players), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.setup_swipe_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(R.string.setup_player_count, state.players.size, MatchSetup.MAX_PLAYERS),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.players, key = { it.id }) { player ->
                ReorderableItem(reorderState, key = player.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "drag")
                    SwipeToRemove(
                        enabled = state.canRemovePlayer,
                        onRemove = { actions.onRemovePlayer(player) }
                    ) {
                        PlayerCard(
                            player = player,
                            errors = playerErrors[player.id] ?: PlayerSetupErrors(),
                            showErrors = state.showErrors,
                            isPractice = state.isPractice,
                            elevation = elevation,
                            onChange = actions.onPlayerChange,
                            onSetStarting = { actions.onSetStartingPlayer(player.id) },
                            dragHandle = {
                                val description = stringResource(R.string.setup_reorder)
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier
                                        .draggableHandle()
                                        .semantics { contentDescription = description }
                                ) {
                                    OutlinedRoundedSymbol(MaterialSymbols.DRAG_INDICATOR)
                                }
                            }
                        )
                    }
                }
            }
            item(key = "add-player") {
                OutlinedButton(
                    onClick = actions.onAddPlayer,
                    enabled = state.canAddPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .animateItem()
                ) {
                    OutlinedRoundedSymbol(MaterialSymbols.PERSON_ADD, size = 20.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.setup_add_player))
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(game: GameState, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val players = game.players
    val headline = when {
        game.isPractice -> stringResource(R.string.setup_resume_practice, players[0].name, formatScore(players[0].score))
        game.isHeadToHead -> stringResource(
            R.string.setup_resume_head_to_head,
            players[0].name, formatScore(players[0].score), formatScore(players[1].score), players[1].name
        )
        else -> players.maxBy { it.score }.let {
            stringResource(R.string.setup_resume_leader, it.name, formatScore(it.score))
        }
    }
    val details = game.targetScore?.let { stringResource(R.string.setup_resume_details, it, game.table.rackNumber) }
        ?: stringResource(R.string.setup_resume_details_practice, game.table.rackNumber)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.setup_resume_label), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(details, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onResume) {
                FilledRoundedSymbol(MaterialSymbols.PLAY_ARROW, size = 18.dp, tint = scheme.onPrimary)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.setup_resume))
            }
        }
    }
}

@Composable
private fun TargetScoreCard(state: SetupUiState, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val error = state.targetScoreError.takeIf { state.showErrors || state.targetScoreText.isNotEmpty() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedRoundedSymbol(MaterialSymbols.TROPHY, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.setup_target_score), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = state.targetScoreText,
                onValueChange = onChange,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
    }
}

@Composable
private fun PracticeCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.tertiaryContainer,
        contentColor = scheme.onTertiaryContainer
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedRoundedSymbol(MaterialSymbols.FITNESS_CENTER, tint = scheme.onTertiaryContainer)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.setup_practice_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.setup_practice_text), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: PlayerSetupItem,
    errors: PlayerSetupErrors,
    showErrors: Boolean,
    isPractice: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onChange: (PlayerSetupItem) -> Unit,
    onSetStarting: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    val nameError = errors.name.takeIf { showErrors || player.name.isNotEmpty() }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = elevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.padding(top = 8.dp)) { dragHandle() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = player.name,
                    onValueChange = { onChange(player.copy(name = it)) },
                    label = { Text(stringResource(R.string.setup_name)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = !isPractice) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = player.headStart,
                            onValueChange = { text ->
                                if (text.length <= 4 && text.all(Char::isDigit)) onChange(player.copy(headStart = text))
                            },
                            label = { Text(stringResource(R.string.setup_head_start)) },
                            isError = errors.headStart != null,
                            supportingText = errors.headStart?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(140.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .selectable(selected = player.isStarting, onClick = onSetStarting, role = Role.RadioButton)
                                .padding(end = 4.dp)
                        ) {
                            RadioButton(selected = player.isStarting, onClick = null, modifier = Modifier.padding(12.dp))
                            Text(
                                stringResource(R.string.setup_breaks),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (player.isStarting) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SetupPreview() {
    StraightScorerTheme {
        SetupContent(
            state = SetupUiState(
                players = listOf(PlayerSetupItem(name = "Alice", isStarting = true), PlayerSetupItem(name = "Bob")),
                targetScoreText = "100"
            ),
            actions = SetupActions()
        )
    }
}
