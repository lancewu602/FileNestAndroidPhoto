package com.filenest.photo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filenest.photo.viewmodel.MainViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

sealed class Screen(val route: String, val title: String) {
    data object Login : Screen("login", "登录")
    data object Browse : Screen("browse", "浏览")
    data object Sync : Screen("sync", "同步")
    data object Settings : Screen("settings", "设置")
    data object AlbumSync : Screen("album_sync", "配置同步相册")
}

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val navController = rememberNavController()

    val startDestination = if (isLoggedIn) Screen.Browse.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Browse.route) { BrowseScreen(navController) }
        composable(Screen.Sync.route) { SyncScreen(navController) }
        composable(Screen.Settings.route) { SettingScreen(navController) }
        composable(Screen.AlbumSync.route) { AlbumSyncScreen(navController) }
    }
}