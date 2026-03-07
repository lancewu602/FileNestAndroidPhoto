package com.filenest.photo.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage

@Composable
fun ZoomableImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        
        containerSize = IntSize(
            (constraints.maxWidth * density.density).toInt(),
            (constraints.maxHeight * density.density).toInt()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            val maxTranslateX = containerSize.width.toFloat() * (scale - 1) * 0.5f
                            val maxTranslateY = containerSize.height.toFloat() * (scale - 1) * 0.5f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxTranslateX, maxTranslateX),
                                y = (offset.y + pan.y).coerceIn(-maxTranslateY, maxTranslateY)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }
    }
}