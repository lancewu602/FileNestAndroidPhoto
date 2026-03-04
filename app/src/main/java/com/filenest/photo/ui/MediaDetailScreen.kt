package com.filenest.photo.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.filenest.photo.util.TimeFormatter
import com.filenest.photo.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("WrongConstant")
fun DetailScreen(
    navController: NavHostController,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val videoPlayerState by viewModel.videoPlayerState.collectAsState()
    var isSystemUiVisible by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.let {
            val window = it.window
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
            }
        }
        onDispose {
            activity?.let {
                val window = it.window
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = true
                }
            }
        }
    }

    BackHandler(enabled = !isSystemUiVisible) {
        isSystemUiVisible = true
        activity?.let {
            val window = it.window
            WindowCompat.getInsetsController(window, window.decorView).apply {
                show(WindowInsets.Type.systemBars())
                isAppearanceLightStatusBars = true
            }
        }
        navController.popBackStack()
    }

    fun toggleSystemUi() {
        isSystemUiVisible = !isSystemUiVisible
        activity?.let {
            val window = it.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isSystemUiVisible) {
                controller.show(WindowInsets.Type.systemBars())
                controller.isAppearanceLightStatusBars = false
            } else {
                controller.hide(WindowInsets.Type.systemBars())
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isSystemUiVisible,
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300))
            ) {
                TopAppBar(
                    title = { Text("详情", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!isSystemUiVisible) {
                                isSystemUiVisible = true
                                activity?.let {
                                    val window = it.window
                                    WindowCompat.getInsetsController(window, window.decorView).apply {
                                        show(WindowInsets.Type.systemBars())
                                        isAppearanceLightStatusBars = false
                                    }
                                }
                            }
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { toggleSystemUi() }
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
                    val imageUrl = "http://192.168.31.55:8916/api/static/media/${media.previewPath}"
                    val videoUrl = "http://192.168.31.55:8916/api/static/media/${media.originalPath}"

                    if (media.type == "VIDEO") {
                        VideoPlayer(
                            exoPlayer = viewModel.exoPlayer,
                            videoUrl = videoUrl,
                            onVideoUrlSet = { viewModel.setVideoUrl(it) },
                            onClick = { toggleSystemUi() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = media.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AnimatedVisibility(
                        visible = isSystemUiVisible,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (media.type == "VIDEO") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = TimeFormatter.formatTimeToHMS(videoPlayerState.currentPosition),
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )

                                        IconButton(
                                            onClick = {
                                                if (videoPlayerState.isPlaying) {
                                                    viewModel.pause()
                                                } else {
                                                    viewModel.play()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (videoPlayerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = if (videoPlayerState.isPlaying) "暂停" else "播放",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Text(
                                            text = TimeFormatter.formatTimeToHMS(videoPlayerState.duration),
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { if (videoPlayerState.duration > 0) videoPlayerState.currentPosition.toFloat() / videoPlayerState.duration else 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black)
                                    .padding(vertical = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FavoriteBorder,
                                            contentDescription = "收藏",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "收藏",
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PhotoLibrary,
                                            contentDescription = "添加到相册",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "添加到相册",
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "删除",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "删除",
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}