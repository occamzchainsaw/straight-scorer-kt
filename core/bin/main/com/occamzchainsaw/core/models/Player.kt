package com.occamzchainsaw.core.models

data class Player (
    val name: String = "",
    val score: Int = 0,
    val currentBreak: Int = 0,
    val headStart: Int = 0,
    val consecutiveFouls: Int = 0,
    val isStarting: Boolean = false,
    val isAtTable: Boolean = false,

    val highestBreak: Int = 0,
    val averageBreak: Float = 0f,
    val breakSum: Int = 0,
    val breakCount: Int = 0,
    val totalFouls: Int = 0
)