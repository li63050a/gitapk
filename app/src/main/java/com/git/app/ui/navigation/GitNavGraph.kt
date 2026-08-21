package com.git.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.git.app.ui.screen.branch.BranchScreen
import com.git.app.ui.screen.commit.CommitScreen
import com.git.app.ui.screen.home.HomeScreen
import com.git.app.ui.screen.remote.RemoteScreen
import com.git.app.ui.screen.settings.SettingsScreen
import com.git.app.ui.screen.ssh.SSHScreen
import com.git.app.ui.screen.stage.StageScreen
import com.git.app.ui.log.LogScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Commit : Screen("commit")
    data object Branch : Screen("branch")
    data object Remote : Screen("remote")
    data object Stage : Screen("stage")
    data object SSH : Screen("ssh")
    data object Settings : Screen("settings")
    data object Log : Screen("log")
}

@Composable
fun GitNavGraph(navController: androidx.navigation.NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(onNavigate = { route -> navController.navigate(route) }) }
        composable(Screen.Commit.route) { CommitScreen() }
        composable(Screen.Branch.route) { BranchScreen() }
        composable(Screen.Remote.route) { RemoteScreen() }
        composable(Screen.Stage.route) { StageScreen() }
        composable(Screen.SSH.route) { SSHScreen() }
        composable(Screen.Settings.route) { SettingsScreen(onNavigateToLog = { navController.navigate(Screen.Log.route) }) }
        composable(Screen.Log.route) { LogScreen(onBack = { navController.popBackStack() }) }
    }
}
