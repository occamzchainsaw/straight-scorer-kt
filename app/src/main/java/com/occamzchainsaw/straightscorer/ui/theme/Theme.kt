package com.occamzchainsaw.straightscorer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun StraightScorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Material You wallpaper colours on Android 12+; otherwise the scheme is generated from [SeedTeal]. */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val seededScheme = rememberDynamicColorScheme(
        seedColor = SeedTeal,
        isDark = darkTheme,
        isAmoled = false,
        style = PaletteStyle.TonalSpot
    )
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        seededScheme
    }

    CompositionLocalProvider(
        LocalOutcomeColors provides if (darkTheme) DarkOutcomeColors else LightOutcomeColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Shortcut for the outcome colours, alongside MaterialTheme.colorScheme. */
val MaterialTheme.outcomeColors: OutcomeColors
    @Composable get() = LocalOutcomeColors.current
