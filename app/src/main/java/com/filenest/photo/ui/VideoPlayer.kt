package com.filenest.photo.ui

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    videoUrl: String,
    onVideoUrlSet: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    LaunchedEffect(videoUrl) {
        onVideoUrlSet(videoUrl)
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false
            }
        },
        modifier = modifier.then(
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
        )
    )
}