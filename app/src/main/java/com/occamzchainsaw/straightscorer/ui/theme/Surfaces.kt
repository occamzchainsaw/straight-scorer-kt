package com.occamzchainsaw.straightscorer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Background for cards and grouped sections. One step up the M3 surface-container scale
 * (lowest, low, default, high, highest) - change it here to adjust card contrast everywhere.
 */
val ColorScheme.cardContainer: Color
    get() = surfaceContainer
