package com.occamzchainsaw.straightscorer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.occamzchainsaw.core.models.BreakEndAction

/** The old MAUI app's primary teal. The whole M3 scheme is generated from it. */
val SeedTeal = Color(0xFF00838F)

/** A colour role for one way a visit can end: an accent plus a container pair for chips and surfaces. */
@Immutable
data class OutcomeColor(
    val color: Color,
    val container: Color,
    val onContainer: Color
)

/** Fixed semantic colours for break end actions, shared by the history graph, buttons and summaries. */
@Immutable
data class OutcomeColors(
    val win: OutcomeColor,
    val safe: OutcomeColor,
    val miss: OutcomeColor,
    val foul: OutcomeColor,
    val breakingFoul: OutcomeColor,
    val thirdFoul: OutcomeColor,
    val breakShot: OutcomeColor
) {
    fun of(action: BreakEndAction): OutcomeColor = when (action) {
        BreakEndAction.Win -> win
        BreakEndAction.Safe -> safe
        BreakEndAction.Miss -> miss
        BreakEndAction.Foul -> foul
        BreakEndAction.BreakingFoul -> breakingFoul
        BreakEndAction.ThirdFoul -> thirdFoul
        BreakEndAction.BreakShot -> breakShot
        BreakEndAction.Finish -> miss
    }
}

val LightOutcomeColors = OutcomeColors(
    win = OutcomeColor(Color(0xFF2E7D32), Color(0xFFC8E6C9), Color(0xFF0D3B12)),
    safe = OutcomeColor(Color(0xFF1565C0), Color(0xFFD6E3FF), Color(0xFF0A2A5E)),
    miss = OutcomeColor(Color(0xFF616161), Color(0xFFE4E4E4), Color(0xFF1F1F1F)),
    foul = OutcomeColor(Color(0xFFC62828), Color(0xFFFFDAD6), Color(0xFF410002)),
    breakingFoul = OutcomeColor(Color(0xFFB26A00), Color(0xFFFFE0B2), Color(0xFF3E2200)),
    thirdFoul = OutcomeColor(Color(0xFF8E0000), Color(0xFFB71C1C), Color(0xFFFFFFFF)),
    breakShot = OutcomeColor(Color(0xFF3949AB), Color(0xFFDEE0FF), Color(0xFF0F1A5C))
)

val DarkOutcomeColors = OutcomeColors(
    win = OutcomeColor(Color(0xFF81C784), Color(0xFF1E4620), Color(0xFFC8E6C9)),
    safe = OutcomeColor(Color(0xFF90CAF9), Color(0xFF0D3A73), Color(0xFFD6E3FF)),
    miss = OutcomeColor(Color(0xFFBDBDBD), Color(0xFF3A3A3A), Color(0xFFE4E4E4)),
    foul = OutcomeColor(Color(0xFFFF8A80), Color(0xFF8C1D18), Color(0xFFFFDAD6)),
    breakingFoul = OutcomeColor(Color(0xFFFFB74D), Color(0xFF5C3A00), Color(0xFFFFE0B2)),
    thirdFoul = OutcomeColor(Color(0xFFFF6E6E), Color(0xFF7F0000), Color(0xFFFFDAD6)),
    breakShot = OutcomeColor(Color(0xFF9FA8DA), Color(0xFF2B3478), Color(0xFFDEE0FF))
)

val LocalOutcomeColors = staticCompositionLocalOf { LightOutcomeColors }
