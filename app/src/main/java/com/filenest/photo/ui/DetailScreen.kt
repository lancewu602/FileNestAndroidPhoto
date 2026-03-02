package com.filenest.photo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.filenest.photo.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavHostController,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "加载失败",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.mediaDetail != null -> {
                    val media = uiState.mediaDetail!!
                    val imageUrl = "http://172.25.20.10:8916/api/preview/media/${media.previewPath}"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = media.name,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "基本信息",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            DetailTextPair(title = "名称", content = media.name)
                            HorizontalDivider()
                            DetailTextPair(title = "类型", content = media.type)
                            HorizontalDivider()
                            DetailTextPair(title = "尺寸", content = "${media.width} x ${media.height}")
                            HorizontalDivider()
                            DetailTextPair(title = "收藏", content = if (media.favorite) "是" else "否")
                            HorizontalDivider()

                            if (media.duration > 0) {
                                DetailTextPair(title = "时长", content = media.durationText)
                                HorizontalDivider()
                            }

                            if (media.inAlbumIds.isNotEmpty()) {
                                DetailTextPair(title = "相册ID", content = media.inAlbumIds.joinToString(", "))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTextPair(title: String, content: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 12.dp)
    )
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}