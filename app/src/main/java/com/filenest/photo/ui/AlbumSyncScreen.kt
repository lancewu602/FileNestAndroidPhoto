package com.filenest.photo.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.filenest.photo.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumSyncScreen(navController: androidx.navigation.NavHostController) {
    val viewModel: MainViewModel = hiltViewModel()
    val albums by viewModel.albums.collectAsState()
    val selectedAlbums by viewModel.selectedAlbums.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAlbums()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置同步相册") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(albums) { album ->
                SwitchTogglePair(
                    title = album.bucketName,
                    checked = album.bucketId in selectedAlbums,
                    onToggle = { viewModel.toggleAlbum(album.bucketId) },
                    count = album.count
                )
                HorizontalDivider()
            }
        }
    }
}