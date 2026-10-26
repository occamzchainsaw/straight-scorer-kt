package com.occamzchainsaw.straightscorer.data.settings

import com.occamzchainsaw.core.models.GameSettings

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** Material You colours on Android 12+. */
    val dynamicColor: Boolean = true,
    /** Keep the screen awake while a game is shown. */
    val keepScreenOn: Boolean = true,
    /** Pre-filled target score for new games. */
    val defaultTargetScore: Int = 100,
    val game: GameSettings = GameSettings()
)
