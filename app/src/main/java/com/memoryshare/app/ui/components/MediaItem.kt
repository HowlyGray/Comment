package com.memoryshare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.memoryshare.app.data.model.Media
import com.memoryshare.app.data.model.MediaType

@Composable
fun MediaItem(
    media: Media,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // For video/audio without a thumbnail we show a dark placeholder instead of
    // trying to decode the media URL as an image (which always fails for video).
    val imageModel: Any? = when {
        media.thumbnailUrl != null -> ImageRequest.Builder(LocalContext.current)
            .data(media.thumbnailUrl)
            .crossfade(true)
            .build()
        media.type == MediaType.IMAGE -> ImageRequest.Builder(LocalContext.current)
            .data(media.url)
            .crossfade(true)
            .build()
        else -> null // VIDEO / AUDIO without thumbnail → dark background only
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = media.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1A))
            )
        }

        if (media.type == MediaType.VIDEO || media.type == MediaType.AUDIO) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Icon(
                    imageVector = if (media.type == MediaType.AUDIO) Icons.Default.AudioFile
                                  else Icons.Default.PlayArrow,
                    contentDescription = "Lecture",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
