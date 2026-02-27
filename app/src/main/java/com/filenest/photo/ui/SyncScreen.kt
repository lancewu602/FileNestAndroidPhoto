package com.filenest.photo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.filenest.photo.viewmodel.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(navController: NavHostController) {
    val viewModel: SyncViewModel = hiltViewModel()
    val syncInfo by viewModel.syncInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSyncInfo()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("同步") }) },
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
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TextContentPair(title = "上次同步时间", content = syncInfo.lastSyncTime)
                    HorizontalDivider()
                    TextContentPair(title = "服务端媒体数量", content = syncInfo.serverMediaCount.toString())
                    HorizontalDivider()
                    TextContentPair(title = "待同步文件数量", content = syncInfo.pendingSyncCount.toString())
                    HorizontalDivider()
                }
            }

            Button(
                onClick = { viewModel.startSync() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !syncInfo.isSyncing
            ) {
                Text(if (syncInfo.isSyncing) "同步中..." else "开始同步")
            }
        }
    }
}