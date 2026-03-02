package com.filenest.photo.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.filenest.photo.ui.components.ProgressContentPair
import com.filenest.photo.ui.components.TextContentPair
import com.filenest.photo.viewmodel.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(navController: NavHostController) {
    val viewModel: SyncViewModel = hiltViewModel()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val serverMediaCount by viewModel.serverMediaCount.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgressInfo by viewModel.syncProgressInfo.collectAsState()
    val syncProgressFile by viewModel.syncProgressFile.collectAsState()
    val syncProgressStep by viewModel.syncProgressStep.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route == Screen.Sync.route) {
            viewModel.loadSyncInfo()
        }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "同步信息",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    TextContentPair(title = "上次同步时间", content = lastSyncTime)
                    HorizontalDivider()
                    TextContentPair(title = "服务端媒体数量", content = serverMediaCount.toString())
                    HorizontalDivider()
                    TextContentPair(title = "待同步文件数量", content = pendingSyncCount.toString())
                    HorizontalDivider()

                    Text(
                        text = "同步状态",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    TextContentPair(
                        title = "状态",
                        content = if (isSyncing) "同步中" else "未开始"
                    )
                    HorizontalDivider()
                    TextContentPair(
                        title = "同步进度",
                        content = "${syncProgressInfo.total}/${syncProgressInfo.completed}"
                    )
                    HorizontalDivider()
                    TextContentPair(
                        title = "文件名称",
                        content = syncProgressInfo.fileName,
                        contentModifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        ellipsizeMiddle = true
                    )
                    HorizontalDivider()
                    ProgressContentPair(
                        title = "文件进度",
                        progressText = "$syncProgressStep ${(syncProgressFile * 100).toInt()}%",
                        progress = syncProgressFile
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Button(
                onClick = { viewModel.startSync() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !isSyncing
            ) {
                Text(if (isSyncing) "同步中..." else "开始同步")
            }
        }
    }
}