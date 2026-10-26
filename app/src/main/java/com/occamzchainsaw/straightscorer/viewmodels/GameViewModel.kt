package com.occamzchainsaw.straightscorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.occamzchainsaw.core.models.Break
import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.models.TableState
import com.occamzchainsaw.core.services.GameEngine
import com.occamzchainsaw.core.services.MatchRecorder
import com.occamzchainsaw.straightscorer.data.settings.AppSettings
import com.occamzchainsaw.straightscorer.data.settings.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Choices offered when a foul is called on an opening break (rules 4, 11). */
enum class OpeningBreakFoul {
    /** A standard foul that happened on the break, e.g. a scratch after a legal break: -1. */
    Standard,

    /** Breaking foul, the incoming player accepts the table: -2. */
    BreakingAcceptTable,

    /** Breaking foul, the incoming player makes the breaker break again: -2. */
    BreakingReBreak
}

data class GameUiState(
    val game: GameState,
    val canUndo: Boolean = false,
    val keepScreenOn: Boolean = true,
    val showOpeningBreakFoulDialog: Boolean = false
) {
    val players: List<Player> get() = game.players
    val atTableIndex: Int get() = game.atTableIndex
    val playerAtTable: Player get() = game.playerAtTable
    val table: TableState get() = game.table
    val breaks: List<Break> get() = game.breaks
    /** Null in practice. */
    val targetScore: Int? get() = game.targetScore
    val isPractice: Boolean get() = game.isPractice
    val isHeadToHead: Boolean get() = game.isHeadToHead
    val isOpeningBreak: Boolean get() = game.table.isOpeningBreak
    val isFinished: Boolean get() = !game.isInProgress
    val winner: Player? get() = game.winner

    /** Points the player at the table still needs to win; null in practice. */
    val pointsToWin: Int? get() = game.targetScore?.let { it - game.playerAtTable.score }

    /** The most points that can be added in one go. */
    val maxPointsAtOnce: Int get() = pointsToWin ?: MAX_PRACTICE_POINTS_AT_ONCE

    companion object {
        const val MAX_PRACTICE_POINTS_AT_ONCE = 999
    }
}

sealed interface GameEvent {
    /** The game was ended (finished, discarded or abandoned) - leave the game screen. */
    data object NavigateHome : GameEvent
}

class GameViewModel(
    private val engine: GameEngine,
    private val recorder: MatchRecorder,
    settingsRepository: SettingsRepository,
    /** True while a game saved before process death is still being loaded. */
    val isRestoring: StateFlow<Boolean> = MutableStateFlow(false)
) : ViewModel() {

    private val openingBreakFoulDialog = MutableStateFlow(false)

    /** Null when there is no game, e.g. right after it was ended. */
    val uiState: StateFlow<GameUiState?> = combine(
        engine.state,
        engine.canUndo,
        settingsRepository.settings,
        openingBreakFoulDialog
    ) { game, canUndo, settings: AppSettings, showFoulDialog ->
        game?.let {
            GameUiState(
                game = it,
                canUndo = canUndo,
                keepScreenOn = settings.keepScreenOn,
                showOpeningBreakFoulDialog = showFoulDialog && it.isInProgress && it.table.isOpeningBreak
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        engine.state.value?.let { GameUiState(game = it, canUndo = engine.canUndo.value) }
    )

    private val _events = Channel<GameEvent>(Channel.BUFFERED)
    val events: Flow<GameEvent> = _events.receiveAsFlow()

    fun addPoint() = engine.addPoints(1)

    /** Adds several balls at once. Returns false (and adds nothing) if [points] is not in 1..maxPointsAtOnce. */
    fun addPoints(points: Int): Boolean {
        val game = engine.state.value ?: return false
        if (!game.isInProgress || points !in 1..GameUiState(game).maxPointsAtOnce) return false
        engine.addPoints(points)
        return true
    }

    fun miss() = engine.miss()

    fun safe() = engine.safe()

    /** Legal opening break with nothing pocketed. */
    fun breakShot() = engine.breakShot()

    /** Ends a practice session; the result is auto-saved and can be discarded from the result sheet. */
    fun finishPractice() = engine.finishPractice()

    /** On an opening break this asks what kind of foul it was; otherwise it's a standard foul. */
    fun foul() {
        val game = engine.state.value ?: return
        if (game.isInProgress && game.table.isOpeningBreak) {
            openingBreakFoulDialog.value = true
        } else {
            engine.foul()
        }
    }

    fun resolveOpeningBreakFoul(choice: OpeningBreakFoul) {
        openingBreakFoulDialog.value = false
        when (choice) {
            OpeningBreakFoul.Standard -> engine.foul()
            OpeningBreakFoul.BreakingAcceptTable -> engine.breakingFoul(reBreak = false)
            OpeningBreakFoul.BreakingReBreak -> engine.breakingFoul(reBreak = true)
        }
    }

    fun dismissOpeningBreakFoulDialog() {
        openingBreakFoulDialog.value = false
    }

    fun undo() = engine.undo()

    /** Finished game: the result is already saved, just close it. */
    fun closeFinishedGame() {
        if (engine.state.value?.isInProgress != false) return
        viewModelScope.launch {
            recorder.close()
            _events.send(GameEvent.NavigateHome)
        }
    }

    /** Finished game: remove the auto-saved result from the history and close the game. */
    fun discardResult() {
        viewModelScope.launch {
            recorder.discard()
            _events.send(GameEvent.NavigateHome)
        }
    }

    /** Ends an unfinished game without saving anything. */
    fun abandonGame() {
        engine.clear()
        _events.trySend(GameEvent.NavigateHome)
    }
}
