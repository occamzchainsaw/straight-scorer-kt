package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.PlayerSetup
import com.occamzchainsaw.core.services.GameEngine
import com.occamzchainsaw.straightscorer.data.settings.SettingsRepository
import com.occamzchainsaw.straightscorer.dtos.PlayerSetupItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerSetupErrors(
    val name: String? = null,
    val headStart: String? = null
) {
    val isValid: Boolean
        get() = name == null && headStart == null
}

data class SetupUiState(
    val players: List<PlayerSetupItem> = emptyList(),
    val targetScoreText: String = "",
    /** Set after a failed start attempt, so the screen can show every error at once. */
    val showErrors: Boolean = false,
    /** A started game that hasn't been finished yet - the home screen offers to resume it. */
    val activeGame: GameState? = null
) {
    val isGameInProgress: Boolean
        get() = activeGame != null

    val targetScore: Int?
        get() = targetScoreText.toIntOrNull()

    /** One player means practice: no target score and no head starts. */
    val isPractice: Boolean
        get() = players.size == 1

    val targetScoreError: String?
        get() {
            if (isPractice) return null
            val score = targetScore
            return when {
                score == null || score < MatchSetup.MIN_TARGET_SCORE ->
                    "Minimum target score is ${MatchSetup.MIN_TARGET_SCORE}"
                score > MatchSetup.MAX_TARGET_SCORE ->
                    "Maximum target score is ${MatchSetup.MAX_TARGET_SCORE}"
                else -> null
            }
        }

    val playerErrors: Map<String, PlayerSetupErrors>
        get() {
            val target = targetScore
            val nameCounts = players.groupingBy { it.name.trim().lowercase() }.eachCount()
            return players.associate { player ->
                val name = player.name.trim()
                val headStart = player.headStartValue
                player.id to PlayerSetupErrors(
                    name = when {
                        name.isEmpty() -> "Name required"
                        name.length > MatchSetup.MAX_NAME_LENGTH ->
                            "Maximum ${MatchSetup.MAX_NAME_LENGTH} characters"
                        (nameCounts[name.lowercase()] ?: 0) > 1 -> "Name already used"
                        else -> null
                    },
                    headStart = when {
                        isPractice -> null
                        headStart == null || headStart < 0 -> "Invalid head start"
                        target != null && headStart >= target -> "Must be less than the target score"
                        else -> null
                    }
                )
            }
        }

    val canAddPlayer: Boolean
        get() = players.size < MatchSetup.MAX_PLAYERS

    val canRemovePlayer: Boolean
        get() = players.size > 1

    val isValid: Boolean
        get() = targetScoreError == null && playerErrors.values.all { it.isValid } && toMatchSetup().isValid

    fun toMatchSetup() = MatchSetup(
        players = players.map { PlayerSetup(name = it.name, headStart = it.headStartValue ?: 0) },
        targetScore = targetScore ?: 0,
        startingPlayerIndex = players.indexOfFirst { it.isStarting }
    )
}

private val PlayerSetupItem.headStartValue: Int?
    get() = if (headStart.isBlank()) 0 else headStart.toIntOrNull()

sealed interface SetupEvent {
    data object NavigateToGame : SetupEvent

    /** Starting would replace an unfinished game - ask first, then call [SetupViewModel.confirmNewGame]. */
    data object ConfirmReplaceGame : SetupEvent
}

class SetupViewModel(
    private val engine: GameEngine,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val form = MutableStateFlow(
        SetupUiState(
            players = listOf(
                PlayerSetupItem(name = "Player 1", isStarting = true),
                PlayerSetupItem(name = "Player 2")
            ),
            targetScoreText = "100"
        )
    )
    private var targetScoreEdited = false

    val uiState: StateFlow<SetupUiState> = combine(form, engine.state) { form, game ->
        form.copy(activeGame = game?.takeIf { it.isInProgress })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, form.value)

    private val _events = Channel<SetupEvent>(Channel.BUFFERED)
    val events: Flow<SetupEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val defaultTarget = settingsRepository.settings.first().defaultTargetScore
            if (!targetScoreEdited) form.update { it.copy(targetScoreText = defaultTarget.toString()) }
        }
    }

    fun updateTargetScore(text: String) {
        if (text.any { !it.isDigit() } || text.length > 4) return
        targetScoreEdited = true
        form.update { it.copy(targetScoreText = text) }
    }

    fun addPlayer() {
        form.update { state ->
            if (!state.canAddPlayer) return@update state
            val usedNames = state.players.map { it.name.trim().lowercase() }.toSet()
            val number = generateSequence(state.players.size + 1) { it + 1 }
                .first { "player $it" !in usedNames }
            state.copy(players = state.players + PlayerSetupItem(name = "Player $number"))
        }
    }

    private var lastRemoved: Pair<Int, PlayerSetupItem>? = null

    fun removePlayer(id: String) {
        form.update { state ->
            val index = state.players.indexOfFirst { it.id == id }
            if (!state.canRemovePlayer || index == -1) return@update state
            lastRemoved = index to state.players[index]
            val remaining = state.players.filterNot { it.id == id }
            state.copy(players = remaining.ensureStartingPlayer())
        }
    }

    /** Puts the last removed player back where they were (as long as there is room). */
    fun undoRemovePlayer() {
        val (index, player) = lastRemoved ?: return
        lastRemoved = null
        form.update { state ->
            if (!state.canAddPlayer || state.players.any { it.id == player.id }) return@update state
            val players = state.players.toMutableList()
            players.add(index.coerceAtMost(players.size), player)
            state.copy(players = players.ensureSingleStartingPlayer(preferred = player.id.takeIf { player.isStarting }))
        }
    }

    /** Applies an edited player. Turning [PlayerSetupItem.isStarting] on turns it off for everyone else. */
    fun updatePlayer(updated: PlayerSetupItem) {
        form.update { state ->
            val previous = state.players.firstOrNull { it.id == updated.id } ?: return@update state
            val players = state.players.map { player ->
                when {
                    player.id == updated.id -> updated
                    updated.isStarting && !previous.isStarting -> player.copy(isStarting = false)
                    else -> player
                }
            }
            state.copy(players = players.ensureStartingPlayer(preferred = previous.id))
        }
    }

    fun setStartingPlayer(id: String) {
        form.update { state ->
            state.copy(players = state.players.map { it.copy(isStarting = it.id == id) })
        }
    }

    /** Moves the player with [id] to where [targetId] currently is (drag and drop). */
    fun movePlayer(id: String, targetId: String) {
        val players = form.value.players
        movePlayer(players.indexOfFirst { it.id == id }, players.indexOfFirst { it.id == targetId })
    }

    /** Reorders players; list order is turn order. */
    fun movePlayer(from: Int, to: Int) {
        form.update { state ->
            if (from !in state.players.indices || to !in state.players.indices) return@update state
            val players = state.players.toMutableList()
            players.add(to, players.removeAt(from))
            state.copy(players = players)
        }
    }

    fun startGame() {
        val state = uiState.value
        if (!state.isValid) {
            form.update { it.copy(showErrors = true) }
            return
        }
        if (state.isGameInProgress) {
            _events.trySend(SetupEvent.ConfirmReplaceGame)
            return
        }
        launchGame(state)
    }

    fun confirmNewGame() {
        val state = uiState.value
        if (state.isValid) launchGame(state)
    }

    fun resumeGame() {
        if (uiState.value.isGameInProgress) _events.trySend(SetupEvent.NavigateToGame)
    }

    private fun launchGame(state: SetupUiState) {
        engine.startGame(state.toMatchSetup())
        form.update { it.copy(showErrors = false) }
        _events.trySend(SetupEvent.NavigateToGame)
    }

    private fun List<PlayerSetupItem>.ensureSingleStartingPlayer(preferred: String?): List<PlayerSetupItem> {
        val starter = preferred ?: firstOrNull { it.isStarting }?.id ?: return ensureStartingPlayer()
        return map { it.copy(isStarting = it.id == starter) }
    }

    /** Someone always has to break - if nobody is selected, fall back to [preferred] or the first player. */
    private fun List<PlayerSetupItem>.ensureStartingPlayer(preferred: String? = null): List<PlayerSetupItem> {
        if (isEmpty() || any { it.isStarting }) return this
        val fallbackId = firstOrNull { it.id == preferred }?.id ?: first().id
        return map { it.copy(isStarting = it.id == fallbackId) }
    }
}
