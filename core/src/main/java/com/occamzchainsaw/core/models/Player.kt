package com.occamzchainsaw.core.models

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val name: String,
    val headStart: Int = 0,
    val score: Int = headStart,
    val currentBreak: Int = 0,
    val consecutiveFouls: Int = 0,

    val highestBreak: Int = 0,
    val breakSum: Int = 0,
    val breakCount: Int = 0,
    val totalFouls: Int = 0
) {
    val averageBreak: Float
        get() = if (breakCount == 0) 0f else breakSum.toFloat() / breakCount
}
