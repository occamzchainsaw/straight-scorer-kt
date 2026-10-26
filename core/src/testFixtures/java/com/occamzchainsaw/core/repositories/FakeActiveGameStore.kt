package com.occamzchainsaw.core.repositories

import com.occamzchainsaw.core.models.GameState

class FakeActiveGameStore(var saved: GameState? = null) : ActiveGameStore {
    var failOnLoad = false
    var saveCount = 0
        private set

    override suspend fun load(): GameState? {
        if (failOnLoad) throw IllegalStateException("corrupt save")
        return saved
    }

    override suspend fun save(state: GameState?) {
        saveCount++
        saved = state
    }
}
