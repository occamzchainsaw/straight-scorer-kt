package com.occamzchainsaw.straightscorer.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.occamzchainsaw.straightscorer.ui.screens.HistoryScreen
import com.occamzchainsaw.straightscorer.ui.screens.MainScreen
import com.occamzchainsaw.straightscorer.ui.screens.RulesScreen
import com.occamzchainsaw.straightscorer.ui.screens.SettingsScreen
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

@Composable
fun StraightScorerApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                TopLevelRoute.entries.forEach { route ->
                    val isSelected =
                        currentDestination?.hierarchy?.any { it.route == route.name } == true

                    NavigationBarItem(
                        icon = {
                            if (isSelected)
                                FilledRoundedSymbol(route.icon)
                            else
                                OutlinedRoundedSymbol(route.icon)
                        },
                        label = {
                            if (isSelected) Text(route.title)
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(route.name) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.Main.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelRoute.Main.name) { MainScreen() }
            composable(TopLevelRoute.History.name) { HistoryScreen() }
            composable(TopLevelRoute.Settings.name) { SettingsScreen() }
            composable(TopLevelRoute.Rules.name) { RulesScreen() }
        }
    }
}