package com.occamzchainsaw.straightscorer.ui.navigation

import dev.vicart.compose.material.symbols.MaterialSymbols

enum class TopLevelRoute(val title: String, val icon: String) {
    Main("Play", MaterialSymbols.PLAY_ARROW),
    History("History", MaterialSymbols.HISTORY),
    Rules("Rules", MaterialSymbols.BALANCE),
    Settings("Settings", MaterialSymbols.SETTINGS)
}