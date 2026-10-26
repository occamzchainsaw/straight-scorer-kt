package com.occamzchainsaw.core.models

data class GameSettings(
    /** Re-rack all 15 balls after a third consecutive foul; the offender then breaks again. */
    val resetRackOnThirdFoul: Boolean = true,
    /** Count pointless visits ending in a miss (or a standard foul) towards the average break. Break shots never count. */
    val countMissesInAverage: Boolean = true,
    /** Count pointless visits ending in a safety towards the average break. */
    val countSafetiesInAverage: Boolean = false
)
