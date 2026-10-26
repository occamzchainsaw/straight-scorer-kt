package com.occamzchainsaw.core.repositories

import com.occamzchainsaw.core.models.GameState

/** Keeps the current game around across app restarts. */
interface ActiveGameStore {
    suspend fun load(): GameState?

    /** Saves the game, or removes the saved one when [state] is null. */
    suspend fun save(state: GameState?)
}
