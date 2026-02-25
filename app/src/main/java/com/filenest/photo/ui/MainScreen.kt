package com.filenest.photo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val title: String) {
    data object Browse : Screen("browse", "浏览")
    data object Sync : Screen("sync", "同步")
    data object Settings : Screen("settings", "设置")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Browse.route
    ) {
        composable(Screen.Browse.route) { BrowseScreen(navController) }
        composable(Screen.Sync.route) { SyncScreen(navController) }
        composable(Screen.Settings.route) { SettingScreen(navController) }
    }
}