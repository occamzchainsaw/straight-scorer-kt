package com.occamzchainsaw.straightscorer.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.occamzchainsaw.straightscorer.R
import com.occamzchainsaw.straightscorer.ui.game.GameScreen
import com.occamzchainsaw.straightscorer.ui.history.HistoryScreen
import com.occamzchainsaw.straightscorer.ui.history.MatchDetailScreen
import com.occamzchainsaw.straightscorer.ui.rules.RulesScreen
import com.occamzchainsaw.straightscorer.ui.settings.LicensesScreen
import com.occamzchainsaw.straightscorer.ui.settings.SettingsScreen
import com.occamzchainsaw.straightscorer.ui.setup.SetupScreen
import com.occamzchainsaw.straightscorer.ui.theme.ScorerMotion
import dev.vicart.compose.material.symbols.FilledRoundedSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol

private const val DELETED_MATCH_KEY = "deleted_match"

enum class TopLevelDestination(@StringRes val label: Int, val route: Any, val symbol: String) {
    Play(R.string.nav_play, GameSetup, MaterialSymbols.PLAY_ARROW),
    History(R.string.nav_history, com.occamzchainsaw.straightscorer.ui.navigation.History, MaterialSymbols.HISTORY),
    Rules(R.string.nav_rules, com.occamzchainsaw.straightscorer.ui.navigation.Rules, MaterialSymbols.MENU_BOOK),
    Settings(R.string.nav_settings, com.occamzchainsaw.straightscorer.ui.navigation.Settings, MaterialSymbols.SETTINGS)
}

private fun NavDestination?.isTopLevel(destination: TopLevelDestination) =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) =
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

@Composable
fun StraightScorerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showNavigation = TopLevelDestination.entries.any { currentDestination.isTopLevel(it) }

    val adaptiveType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())

    NavigationSuiteScaffold(
        layoutType = if (showNavigation) adaptiveType else NavigationSuiteType.None,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination.isTopLevel(destination)
                item(
                    selected = selected,
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = {
                        if (selected) FilledRoundedSymbol(destination.symbol)
                        else OutlinedRoundedSymbol(destination.symbol)
                    },
                    label = { Text(stringResource(destination.label)) }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = GameSetup,
            enterTransition = { fadeIn(tween(ScorerMotion.MEDIUM, easing = ScorerMotion.EmphasizedDecelerate)) },
            exitTransition = { fadeOut(tween(ScorerMotion.SHORT, easing = ScorerMotion.EmphasizedAccelerate)) }
        ) {
            composable<GameSetup> {
                SetupScreen(onNavigateToGame = { navController.navigate(Game) { launchSingleTop = true } })
            }
            composable<Game> {
                GameScreen(
                    onNavigateHome = {
                        if (!navController.popBackStack(GameSetup, inclusive = false)) {
                            navController.navigateToTopLevel(TopLevelDestination.Play)
                        }
                    }
                )
            }
            composable<History> { entry ->
                val pendingDeleteId by entry.savedStateHandle
                    .getStateFlow<String?>(DELETED_MATCH_KEY, null)
                    .collectAsState()
                HistoryScreen(
                    onOpenMatch = { id -> navController.navigate(MatchDetail(id)) },
                    pendingDeleteId = pendingDeleteId,
                    onPendingDeleteHandled = { entry.savedStateHandle[DELETED_MATCH_KEY] = null }
                )
            }
            composable<MatchDetail> {
                MatchDetailScreen(
                    onBack = { navController.popBackStack() },
                    onDelete = { id ->
                        // the history screen deletes it so it can offer undo
                        navController.previousBackStackEntry?.savedStateHandle?.set(DELETED_MATCH_KEY, id)
                        navController.popBackStack()
                    }
                )
            }
            composable<Settings> { SettingsScreen(onOpenLicenses = { navController.navigate(Licenses) }) }
            composable<Licenses> { LicensesScreen(onBack = { navController.popBackStack() }) }
            composable<Rules> { RulesScreen() }
        }
    }
}
