package com.occamzchainsaw.straightscorer.ui.rules

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.data.rules.RulesDocument
import com.occamzchainsaw.straightscorer.data.rules.TocEntry
import com.occamzchainsaw.straightscorer.viewmodels.AppViewModelProvider
import com.occamzchainsaw.straightscorer.viewmodels.RulesSearchState
import com.occamzchainsaw.straightscorer.viewmodels.RulesUiState
import com.occamzchainsaw.straightscorer.viewmodels.RulesViewModel
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol
import kotlinx.coroutines.launch

/** Comfortable line length for reading. */
private val ReadingWidth = 680.dp

@Immutable
data class RulesActions(
    val onOpenSearch: () -> Unit = {},
    val onCloseSearch: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onNextMatch: () -> Unit = {},
    val onPreviousMatch: () -> Unit = {}
)

@Composable
fun RulesScreen(viewModel: RulesViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val uiState by viewModel.uiState.collectAsState()
    val actions = remember(viewModel) {
        RulesActions(
            onOpenSearch = viewModel::openSearch,
            onCloseSearch = viewModel::closeSearch,
            onQueryChange = viewModel::setQuery,
            onNextMatch = viewModel::nextMatch,
            onPreviousMatch = viewModel::previousMatch
        )
    }
    RulesContent(uiState, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesContent(
    state: RulesUiState,
    actions: RulesActions,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    initiallyShowContents: Boolean = false
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var showContents by rememberSaveable { mutableStateOf(initiallyShowContents) }
    val loaded = state as? RulesUiState.Loaded
    val search = loaded?.search ?: RulesSearchState()

    BackHandler(enabled = search.isActive) { actions.onCloseSearch() }

    // step through search results
    LaunchedEffect(search.currentMatch) {
        search.currentMatch?.let { listState.animateScrollToItem(it.blockIndex) }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (search.isActive) {
                SearchTopBar(search, actions)
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_rules)) },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (loaded != null) {
                            ToolbarIcon(MaterialSymbols.SEARCH, stringResource(R.string.rules_search), onClick = actions.onOpenSearch)
                            ToolbarIcon(MaterialSymbols.TOC, stringResource(R.string.rules_contents)) { showContents = true }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
            AnimatedVisibility(visible = showBackToTop, enter = scaleIn(), exit = scaleOut()) {
                val description = stringResource(R.string.rules_back_to_top)
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.semantics { contentDescription = description }
                ) {
                    FilledRoundedSymbol(MaterialSymbols.ARROW_UPWARD)
                }
            }
        }
    ) { padding ->
        when (state) {
            RulesUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            RulesUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.rules_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is RulesUiState.Loaded -> {
                val document = state.document
                val onLink: (String) -> Unit = { url ->
                    if (url.startsWith("#")) {
                        document.resolveAnchor(url)?.let { index -> scope.launch { listState.animateScrollToItem(index) } }
                    } else {
                        runCatching { uriHandler.openUri(url) }
                    }
                }
                RulesDocumentList(
                    document = document,
                    search = state.search,
                    onLink = onLink,
                    listState = listState,
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 88.dp
                    )
                )
                if (showContents) {
                    ContentsSheet(
                        entries = document.tableOfContents,
                        onSelect = { entry ->
                            showContents = false
                            scope.launch { listState.animateScrollToItem(entry.blockIndex) }
                        },
                        onDismiss = { showContents = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesDocumentList(
    document: RulesDocument,
    search: RulesSearchState,
    onLink: (String) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues
) {
    val highlightsByBlock = remember(search.matches, search.currentIndex) {
        val current = search.currentMatch
        search.matches.groupBy { it.blockIndex }.mapValues { (_, matches) ->
            matches.groupBy({ it.segmentIndex }) { Highlight(it.range, isCurrent = it == current) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(document.blocks, key = { index, _ -> index }) { index, block ->
            val blockHighlights = highlightsByBlock[index]
            val context = remember(blockHighlights, onLink) {
                BlockRenderContext(onLink) { segment -> blockHighlights?.get(segment).orEmpty() }
            }
            MarkdownBlock(
                block = block,
                context = context,
                modifier = Modifier
                    .widthIn(max = ReadingWidth)
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(search: RulesSearchState, actions: RulesActions) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    TopAppBar(
        navigationIcon = {
            ToolbarIcon(MaterialSymbols.ARROW_BACK, stringResource(R.string.rules_close_search), onClick = actions.onCloseSearch)
        },
        title = {
            TextField(
                value = search.query,
                onValueChange = actions.onQueryChange,
                placeholder = { Text(stringResource(R.string.rules_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { actions.onNextMatch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        actions = {
            if (search.query.length >= 2) {
                Text(
                    text = if (search.matches.isEmpty()) stringResource(R.string.rules_no_matches)
                    else stringResource(R.string.rules_match_count, search.currentIndex + 1, search.matches.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ToolbarIcon(
                MaterialSymbols.KEYBOARD_ARROW_UP,
                stringResource(R.string.rules_previous_match),
                enabled = search.matches.isNotEmpty(),
                onClick = actions.onPreviousMatch
            )
            ToolbarIcon(
                MaterialSymbols.KEYBOARD_ARROW_DOWN,
                stringResource(R.string.rules_next_match),
                enabled = search.matches.isNotEmpty(),
                onClick = actions.onNextMatch
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentsSheet(entries: List<TocEntry>, onSelect: (TocEntry) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            text = stringResource(R.string.rules_contents),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(entries, key = { _, entry -> entry.blockIndex }) { _, entry ->
                Text(
                    text = entry.title,
                    style = if (entry.level == 1) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry.level == 1) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (entry.level == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(entry) }
                        .padding(start = 24.dp + 16.dp * (entry.level - 1).coerceAtMost(2), end = 24.dp, top = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolbarIcon(symbol: String, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = description }) {
        OutlinedRoundedSymbol(
            symbol,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}
