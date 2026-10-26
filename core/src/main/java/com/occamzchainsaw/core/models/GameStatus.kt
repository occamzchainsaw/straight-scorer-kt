package com.occamzchainsaw.core.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface GameStatus {
    @Serializable
    data object InProgress : GameStatus

    @Serializable
    /** [winnerId] is null when a practice session was finished. */
    data class Finished(val winnerId: Int?) : GameStatus
}
