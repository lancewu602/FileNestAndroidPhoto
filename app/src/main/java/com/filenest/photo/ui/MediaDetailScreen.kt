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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.filenest.photo.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("WrongConstant")
fun DetailScreen(
    navController: NavHostController,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
                    val imageUrl = "http://172.25.20.14:8916/api/preview/media/${media.previewPath}"
                    val videoUrl = "http://172.25.20.14:8916/api/preview/media/${media.originalPath}"

                    if (media.type == "VIDEO") {
                        VideoPlayer(
                            videoUrl = videoUrl,
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
                }
            }
        }
    }
}