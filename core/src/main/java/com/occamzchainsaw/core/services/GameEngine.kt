package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.models.GameAction
import com.occamzchainsaw.core.models.GameSettings
import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.models.MatchSetup
import com.occamzchainsaw.core.models.Player
import com.occamzchainsaw.core.rules.GameReducer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current game and its undo history. All rules live in [GameReducer];
 * undo works by keeping snapshots of previous states.
 */
class GameEngine(
    private val settings: () -> GameSettings = { GameSettings() },
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxUndoSteps: Int = 100
) {
    private val lock = Any()
    private val undoStack = ArrayDeque<GameState>()

    private val _state = MutableStateFlow<GameState?>(null)
    private val _canUndo = MutableStateFlow(false)

    /** Current game, or null when no game has been started (or it was abandoned). */
    val state: StateFlow<GameState?> = _state.asStateFlow()
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    fun startGame(setup: MatchSetup) {
        val errors = setup.validate()
        require(errors.isEmpty()) { "Invalid match setup: $errors" }

        replaceState(
            GameState(
                players = setup.players.mapIndexed { index, player ->
                    Player(
                        id = index,
                        name = player.name.trim(),
                        headStart = if (setup.isPractice) 0 else player.headStart
                    )
                },
                atTableIndex = setup.startingPlayerIndex,
                targetScore = setup.targetScore.takeUnless { setup.isPractice },
                startedAt = clock()
            )
        )
    }

    /**
     * Restores a previously saved game (e.g. after process death), unless a game has been started in the meantime.
     * Undo history is not restored. Returns true if the game was restored.
     */
    fun restore(state: GameState): Boolean {
        synchronized(lock) {
            if (_state.value != null) return false
            replaceState(state)
            return true
        }
    }

    /** Ends the current game without a result. */
    fun clear() = replaceState(null)

    fun dispatch(action: GameAction) {
        synchronized(lock) {
            val current = _state.value ?: return
            val next = GameReducer.reduce(current, action, settings(), clock())
            if (next == current) return

            undoStack.addLast(current)
            if (undoStack.size > maxUndoSteps) undoStack.removeFirst()
            _state.value = next
            _canUndo.value = true
        }
    }

    fun addPoints(points: Int = 1) = dispatch(GameAction.AddPoints(points))
    fun miss() = dispatch(GameAction.Miss)
    fun safe() = dispatch(GameAction.Safe)
    fun foul() = dispatch(GameAction.Foul)
    fun breakingFoul(reBreak: Boolean) = dispatch(GameAction.BreakingFoul(reBreak))
    fun breakShot() = dispatch(GameAction.BreakShot)
    fun finishPractice() = dispatch(GameAction.FinishPractice)

    /** Reverts the last action, including the one that won the game. */
    fun undo() {
        synchronized(lock) {
            val previous = undoStack.removeLastOrNull() ?: return
            _state.value = previous
            _canUndo.value = undoStack.isNotEmpty()
        }
    }

    private fun replaceState(state: GameState?) {
        synchronized(lock) {
            undoStack.clear()
            _state.value = state
            _canUndo.value = false
        }
    }
}
