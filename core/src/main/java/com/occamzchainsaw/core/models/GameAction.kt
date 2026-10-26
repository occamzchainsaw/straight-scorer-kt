package com.occamzchainsaw.core.models

sealed interface GameAction {
    data class AddPoints(val points: Int = 1) : GameAction
    data object Miss : GameAction
    data object Safe : GameAction

    /** Standard foul: -1, counts towards three consecutive fouls. */
    data object Foul : GameAction

    /**
     * Foul on the opening break: -2, does not count towards three consecutive fouls.
     * [reBreak] - the incoming player requires the breaker to break again instead of accepting the table.
     */
    data class BreakingFoul(val reBreak: Boolean) : GameAction

    /** A legal opening break with no ball pocketed - the turn passes. */
    data object BreakShot : GameAction

    /** Ends a practice session; ignored in a match. */
    data object FinishPractice : GameAction
}
