package com.filenest.photo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.filenest.photo.ui.components.TextContentPair
import com.filenest.photo.ui.components.TextContentPairClickable
import com.filenest.photo.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(navController: NavHostController) {
    val viewModel: MainViewModel = hiltViewModel()
    val serverUrl by viewModel.getServerUrl().collectAsState(initial = "")
    val username by viewModel.getUsername().collectAsState(initial = "")

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextContentPair(title = "服务器地址", content = serverUrl)
                    HorizontalDivider()
                    TextContentPair(title = "用户名", content = username)
                    HorizontalDivider()
                    TextContentPairClickable(
                        title = "配置同步相册",
                        onClick = { navController.navigate("album_sync") },
                        endIcon = Icons.Filled.ChevronRight
                    )
                    HorizontalDivider()
                }
            }

            Button(
                onClick = {
                    viewModel.logout {
                        navController.navigate("login") {
                            popUpTo("settings") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("退出登录")
            }
        }
    }
}