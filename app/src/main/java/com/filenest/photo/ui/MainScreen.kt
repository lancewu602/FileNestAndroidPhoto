package com.filenest.photo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
}

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = hiltViewModel()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = null)
    val navController = rememberNavController()
    var hasInitialNavigation by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (!hasInitialNavigation && isLoggedIn != null) {
            hasInitialNavigation = true
            val route = if (isLoggedIn == true) Screen.Browse.route else Screen.Login.route
            navController.navigate(route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoggedIn == null) {
            CircularProgressIndicator()
        } else {
            NavHost(
                navController = navController,
                startDestination = Screen.Login.route
            ) {
                composable(Screen.Login.route) { LoginScreen(navController) }
                composable(Screen.Browse.route) { BrowseScreen(navController) }
                composable(Screen.Sync.route) { SyncScreen(navController) }
                composable(Screen.Settings.route) { SettingScreen(navController) }
            }
        }
    }
}