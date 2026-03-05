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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.filenest.photo.util.TimeFormatter
import com.filenest.photo.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("WrongConstant")
fun DetailScreen(
    navController: NavHostController,
    mediaId: Int,
    mediaData: String,
    viewModel: DetailViewModel = hiltViewModel()
) {
    viewModel.setInitialData(mediaId, mediaData)
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    var immersiveMode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as Activity

    val view = LocalView.current
    val windowInsetsController = view.windowInsetsController

    // 默认设置为亮色
    windowInsetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)

    fun goBack() {
        windowInsetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
        if (immersiveMode) {
            windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            windowInsetsController?.show(WindowInsets.Type.systemBars())
        }
        navController.popBackStack()
    }

    BackHandler {
        goBack()
    }

    fun toggleImmersiveMode() {
        immersiveMode = !immersiveMode
        if (immersiveMode) {
            windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            windowInsetsController?.show(WindowInsets.Type.systemBars())
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !immersiveMode,
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300))
            ) {
                TopAppBar(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                    title = { Text("详情", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            goBack()
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
                ) { toggleImmersiveMode() }
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
                    val imageUrl = "http://172.25.20.10:8916/api/static/media/${media.previewPath}"
                    val videoUrl = "http://172.25.20.10:8916/api/static/media/${media.originalPath}"

                    if (media.type == "VIDEO") {
                        VideoPlayer(
                            exoPlayer = viewModel.exoPlayer,
                            videoUrl = videoUrl,
                            onVideoUrlSet = { viewModel.setVideoUrl(it) },
                            onClick = { toggleImmersiveMode() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ZoomableImage(
                            imageUrl = imageUrl,
                            contentDescription = media.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AnimatedVisibility(
                        visible = !immersiveMode,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {}
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
                                            text = TimeFormatter.formatTimeToHMS(currentPosition),
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )

                                        IconButton(
                                            onClick = {
                                                if (isPlaying) {
                                                    viewModel.pause()
                                                } else {
                                                    viewModel.play()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Text(
                                            text = TimeFormatter.formatTimeToHMS(duration),
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
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
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = { }) {
                                    Icon(
                                        imageVector = Icons.Filled.FavoriteBorder,
                                        contentDescription = "收藏",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { }) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = "添加到相册",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { }) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "详细信息",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "删除",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
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