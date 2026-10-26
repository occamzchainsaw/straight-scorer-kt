package com.occamzchainsaw.core.models

import kotlinx.serialization.Serializable

@Serializable
data class TableState(
    val ballsOnTable: Int = 15,
    val rackNumber: Int = 1,
    /** True when the next shot has to meet the opening break requirements. */
    val isOpeningBreak: Boolean = true
)
