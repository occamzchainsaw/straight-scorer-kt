package com.occamzchainsaw.core.models

import java.util.UUID

data class MatchResult (
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val winner: PlayerMatchSummary? = null,
    val players: List<PlayerMatchSummary> = emptyList()
)