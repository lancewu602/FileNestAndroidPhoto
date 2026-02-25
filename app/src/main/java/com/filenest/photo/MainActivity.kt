package com.filenest.photo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.api.resetApiService
import com.filenest.photo.theme.MainTheme
import com.filenest.photo.ui.MainScreen
import com.filenest.photo.ui.login.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 简单的启动检查逻辑：阻塞式获取 Token 决定初始页面（实际项目中建议使用 Splash Screen + 异步检查）
        var startDestination = "login"
        runBlocking {
            val token = AppPrefKeys.getServerToken(applicationContext).first()
            val domain = AppPrefKeys.getServerDomain(applicationContext).first()
            if (token.isNotEmpty() && domain.isNotEmpty()) {
                try {
                    resetApiService(domain, token)
                    startDestination = "main"
                } catch (e: Exception) {
                    // 如果初始化失败，回退到登录
                    startDestination = "login"
                }
            }
        }

        setContent {
            MainTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main") {
                        MainScreen()
                    }
                }
            }
        }
    }
}


