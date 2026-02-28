package com.filenest.photo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TextContentPair(
    title: String,
    content: String,
    contentModifier: Modifier = Modifier,
    ellipsizeMiddle: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        AnimatedContent(
            targetState = content,
            transitionSpec = {
                slideInVertically { height -> height } togetherWith
                    slideOutVertically { height -> -height }
            },
            label = "content",
            modifier = Modifier
        ) { targetContent ->
            Text(
                text = targetContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = if (ellipsizeMiddle) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis,
                modifier = contentModifier
            )
        }
    }
}