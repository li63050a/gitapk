package com.git.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.git.app.data.RepoManager
import java.net.URLDecoder
import java.net.URLEncoder
import com.git.app.ui.screen.GitConfigScreen
import com.git.app.ui.screen.AboutScreen
import com.git.app.ui.screen.HomeScreen
import com.git.app.ui.screen.RepoDetailScreen
import com.git.app.ui.log.LogScreen
import com.git.app.ui.screen.settings.SettingsScreen
import com.git.app.ui.screen.ssh.SSHScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object RepoDetail : Screen("repoDetail:{path}")
    data object GitConfig : Screen("gitConfig:{path}")
    data object Settings : Screen("settings")
    data object Log : Screen("log")
    data object About : Screen("about")
    data object Ssh : Screen("ssh")
}

@Composable
fun GitNavGraph(
    navController: NavHostController,
    repoManager: RepoManager
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onRepoSelected = { path ->
                    repoManager.selectRepo(path)
                    navController.navigate("repoDetail:${URLEncoder.encode(path, "UTF-8")}")
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToConfig = { path ->
                    navController.navigate("gitConfig:${URLEncoder.encode(path, "UTF-8")}")
                }
            )
        }
        composable(Screen.RepoDetail.route) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("path") ?: return@composable
            val path = URLDecoder.decode(encoded, "UTF-8")
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
        composable(Screen.GitConfig.route) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("path") ?: return@composable
            val path = URLDecoder.decode(encoded, "UTF-8")
            GitConfigScreen(
                repoPath = path,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToLog = { navController.navigate(Screen.Log.route) },
                onNavigateToSsh = { navController.navigate(Screen.Ssh.route) }
            )
        }
        composable(Screen.Log.route) {
            LogScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Ssh.route) {
            SSHScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
