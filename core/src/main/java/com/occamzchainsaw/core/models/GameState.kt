package com.occamzchainsaw.core.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GameState(
    /** Also used as the id of the saved [MatchResult], so re-finishing after an undo overwrites it. */
    val id: String = UUID.randomUUID().toString(),
    val players: List<Player>,
    val atTableIndex: Int,
    /** Null in practice mode: no target and no winner. */
    val targetScore: Int?,
    val table: TableState = TableState(),
    val breaks: List<Break> = emptyList(),
    val status: GameStatus = GameStatus.InProgress,
    val startedAt: Long,
    val finishedAt: Long? = null
) {
    val playerAtTable: Player
        get() = players[atTableIndex]

    val isInProgress: Boolean
        get() = status == GameStatus.InProgress

    val winner: Player?
        get() = (status as? GameStatus.Finished)?.let { finished ->
            players.firstOrNull { it.id == finished.winnerId }
        }

    val isHeadToHead: Boolean
        get() = players.size == 2

    val isPractice: Boolean
        get() = targetScore == null
}
