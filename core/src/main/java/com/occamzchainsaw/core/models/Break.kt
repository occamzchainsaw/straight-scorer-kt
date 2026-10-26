package com.occamzchainsaw.core.models

import kotlinx.serialization.Serializable

enum class BreakEndAction {
    Safe,
    Miss,
    Foul,
    BreakingFoul,
    ThirdFoul,
    Win,

    /** A legal opening break with nothing pocketed. */
    BreakShot,

    /** The visit that was running when a practice session was finished. */
    Finish
}

/**
 * A single visit to the table.
 * [points] are balls pocketed during the visit, [penalty] is what was subtracted for a foul.
 */
@Serializable
data class Break(
    val playerId: Int,
    val playerName: String,
    val points: Int,
    val penalty: Int = 0,
    val endAction: BreakEndAction,
    val rackNumber: Int
) {
    val netPoints: Int
        get() = points - penalty
}
