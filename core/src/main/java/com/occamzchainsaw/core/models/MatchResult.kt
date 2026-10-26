package com.occamzchainsaw.core.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MatchResult(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long,
    val finishedAt: Long,
    /** Null for practice sessions. */
    val targetScore: Int?,
    val racks: Int,
    val players: List<PlayerMatchSummary>,
    val breaks: List<Break> = emptyList()
) {
    val winner: PlayerMatchSummary?
        get() = players.firstOrNull { it.isWinner }

    val isPractice: Boolean
        get() = targetScore == null
}
