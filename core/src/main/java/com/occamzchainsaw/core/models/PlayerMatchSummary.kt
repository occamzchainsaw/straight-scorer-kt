package com.occamzchainsaw.core.models

data class PlayerMatchSummary(
    var name: String = "",
    var isWinner: Boolean = false,
    var score: Int = 0,
    var highestBreak: Int = 0,
    var averageBreak: Float = 0f,
    var totalFouls: Int = 0
)
