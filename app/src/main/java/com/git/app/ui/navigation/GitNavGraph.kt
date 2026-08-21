package com.git.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.git.app.data.RepoManager
import com.git.app.ui.screen.RepoDetailScreen
import com.git.app.ui.log.LogScreen
import com.git.app.ui.screen.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object RepoDetail : Screen("repoDetail:{path}")
    data object Settings : Screen("settings")
    data object Log : Screen("log")
}

@Composable
fun GitNavGraph(
    navController: NavHostController,
    repoManager: RepoManager
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.RepoDetail.route) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path") ?: return@composable
            val repo = repoManager.repos.find { it.path == path }
            if (repo != null) {
                RepoDetailScreen(
                    repoPath = repo.path,
                    repoName = repo.name,
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    repoManager = repoManager
                )
            }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateToLog = { navController.navigate(Screen.Log.route) })
        }
        composable(Screen.Log.route) {
            LogScreen(onBack = { navController.popBackStack() })
        }
    }
}
