package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.models.GameState
import com.occamzchainsaw.core.repositories.MatchHistoryRepository
import com.occamzchainsaw.core.rules.toMatchResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Auto-saves finished games to the match history.
 * Undoing the winning shot removes the saved result again; [discard] drops it on request.
 */
class MatchRecorder(
    private val engine: GameEngine,
    private val repository: MatchHistoryRepository
) {
    private val mutex = Mutex()
    private var savedGameId: String? = null

    /** Observes the engine until cancelled - launch it in a long-lived scope. */
    suspend fun run() {
        engine.state.collect { onStateChanged(it) }
    }

    /** Ends a finished game, making sure its result is saved even if [run] hasn't caught up yet. */
    suspend fun close() {
        mutex.withLock {
            val result = engine.state.value?.toMatchResult() ?: return
            repository.save(result)
            savedGameId = result.id
            engine.clear()
        }
    }

    /** Removes the finished game's result from the history and ends the game. */
    suspend fun discard() {
        mutex.withLock {
            val state = engine.state.value?.takeUnless { it.isInProgress } ?: return
            engine.clear()
            repository.delete(state.id)
            if (savedGameId == state.id) savedGameId = null
        }
    }

    private suspend fun onStateChanged(state: GameState?) {
        mutex.withLock {
            val result = state?.toMatchResult()
            val previouslySaved = savedGameId

            when {
                result != null -> {
                    repository.save(result)
                    savedGameId = result.id
                }
                state != null && state.id == previouslySaved -> {
                    // the winning action was undone
                    repository.delete(state.id)
                    savedGameId = null
                }
                else -> savedGameId = null
            }
        }
    }
}
