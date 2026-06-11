package com.occamzchainsaw.straightscorer.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.occamzchainsaw.straightscorer.ui.screens.gameScreen
import com.occamzchainsaw.straightscorer.ui.screens.setupScreen
import com.occamzchainsaw.straightscorer.ui.screens.historyScreen
import com.occamzchainsaw.straightscorer.ui.screens.rulesScreen
import com.occamzchainsaw.straightscorer.ui.screens.settingsScreen
import com.occamzchainsaw.straightscorer.viewmodels.GameViewModel
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

data class BottomNavItem(val title: String, val route: Any, val symbol: String)

val bottomNavItems = listOf(
    BottomNavItem("Setup", GameSetup, MaterialSymbols.PLAY_ARROW),
    BottomNavItem("Rules", Rules, MaterialSymbols.BALANCE),
    BottomNavItem("History", History, MaterialSymbols.HISTORY),
    BottomNavItem("Settings", Settings, MaterialSymbols.SETTINGS)
)

@Composable
fun StraightScorerApp() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()
    val isMatchActive by gameViewModel.isMatchActive.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomNav = bottomNavItems.any {item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true

                        NavigationBarItem(
                            selected = isSelected,
                            label = { Text(item.title) },
                            icon = {
                                if (isSelected) FilledRoundedSymbol(item.symbol)
                                else OutlinedRoundedSymbol(item.symbol)
                            },
                            onClick = {
                                navController.navigate(item.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = GameSetup,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<GameSetup> {
                setupScreen(
                    navController = navController,
                    viewModel = gameViewModel
                )
            }
            composable<Game> {
                gameScreen(
                    navController = navController,
                    viewModel = gameViewModel,
                    availableRoutes = bottomNavItems
                )
            }
            composable<History> { historyScreen() }
            composable<Settings> { settingsScreen() }
            composable<Rules> { rulesScreen() }
        }
    }
}
