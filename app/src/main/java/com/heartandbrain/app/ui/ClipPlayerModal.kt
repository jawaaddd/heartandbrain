package com.heartandbrain.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.heartandbrain.app.data.Clip
import com.heartandbrain.app.data.ClipType
import com.heartandbrain.app.ui.theme.Primary
import com.heartandbrain.app.ui.theme.Subtle

@Composable
fun ClipPlayerModal(
    clip: Clip,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val context = LocalContext.current
    var isPinned by remember { mutableStateOf(clip.isPinned) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.Builder()
                .setUri(clip.filePath)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs((clip.startTime * 1000).toLong())
                        .setEndPositionMs((clip.endTime * 1000).toLong())
                        .build()
                )
                .build()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column {
                // Video
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color.Black),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Pin button — top-right corner of the video
                    IconButton(
                        onClick = {
                            isPinned = !isPinned
                            onTogglePin()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            tint = if (isPinned) Primary else Color.White.copy(alpha = 0.4f),
                        )
                    }
                }

                // Info row
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = if (clip.type == ClipType.QUOTE)
                            "\u201C${clip.quoteText ?: clip.title}\u201D"
                        else
                            clip.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = if (clip.type == ClipType.QUOTE) FontStyle.Italic else FontStyle.Normal,
                        lineHeight = 22.sp,
                    )
                    Text(
                        text = clip.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = Subtle,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
