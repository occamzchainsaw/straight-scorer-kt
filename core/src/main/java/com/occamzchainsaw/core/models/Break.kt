package com.occamzchainsaw.core.models

enum class BreakEndAction {
    Safe,
    Miss,
    Foul,
    ThirdFoul,
    Win
}

data class Break (
    val player: Player = Player(),
    val pointsScored: Int = 0,
    val breakEndAction: BreakEndAction = BreakEndAction.Safe
)