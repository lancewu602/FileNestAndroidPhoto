package com.filenest.photo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val title: String) {
    data object Browse : Screen("browse", "浏览")
    data object Sync : Screen("sync", "同步")
    data object Settings : Screen("settings", "设置")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

}

