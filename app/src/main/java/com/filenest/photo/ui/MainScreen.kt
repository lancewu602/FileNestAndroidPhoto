package com.filenest.photo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

sealed class Screen(val route: String, val title: String) {
    data object Welcome : Screen("welcome", "欢迎")
    data object Login : Screen("login", "登录")
    data object Browse : Screen("browse", "浏览")
    data object Album : Screen("album", "相册")
    data object Sync : Screen("sync", "同步")
    data object Settings : Screen("settings", "设置")
    data object AlbumSync : Screen("album_sync", "配置同步相册")
    data object Detail : Screen("detail/{mediaId}?data={data}", "详情") {
        fun createRoute(mediaId: Int, data: String = "") = "detail/$mediaId" + if (data.isNotEmpty()) "?data=$data" else ""
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToBrowse = {
                    navController.navigate(Screen.Browse.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Browse.route) { BrowseScreen(navController) }
        composable(Screen.Album.route) { AlbumScreen(navController) }
        composable(Screen.Sync.route) { SyncScreen(navController) }
        composable(Screen.Settings.route) { SettingScreen(navController) }
        composable(Screen.AlbumSync.route) { AlbumSyncScreen(navController) }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.IntType },
                navArgument("data") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: 0
            val data = backStackEntry.arguments?.getString("data") ?: ""
            DetailScreen(navController, mediaId, data)
        }
    }
}