package com.occamzchainsaw.core.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerMatchSummary(
    val name: String,
    val headStart: Int = 0,
    val finalScore: Int,
    val highestBreak: Int = 0,
    val averageBreak: Float = 0f,
    val breakCount: Int = 0,
    val totalFouls: Int = 0,
    val isWinner: Boolean = false
)
