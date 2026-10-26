package com.occamzchainsaw.core.services

import com.occamzchainsaw.core.repositories.ActiveGameStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

/** Restores the last game on start-up, then saves every change so it survives process death. */
class ActiveGamePersister(
    private val engine: GameEngine,
    private val store: ActiveGameStore
) {
    private val _isRestoring = MutableStateFlow(true)

    /** True until the saved game (if any) has been loaded into the engine. */
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    /** Observes the engine until cancelled - launch it in a long-lived scope. */
    suspend fun run() {
        restore()
        engine.state.collect { state ->
            try {
                store.save(state)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // losing one snapshot is not worth crashing the game over; the next change saves again
            }
        }
    }

    private suspend fun restore() {
        val saved = try {
            store.load()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null // unreadable or outdated save - start fresh
        }
        saved?.let(engine::restore)
        _isRestoring.value = false
    }
}
