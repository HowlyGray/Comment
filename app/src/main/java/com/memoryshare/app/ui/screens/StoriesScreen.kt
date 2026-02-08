package com.memoryshare.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.Story
import com.memoryshare.app.data.model.StoryMediaType
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StoriesScreen(
    stories: List<Story>,
    author: User?,
    currentIndex: Int,
    onStoryChange: (Int) -> Unit,
    onClose: () -> Unit,
    onMarkAsViewed: (Story) -> Unit
) {
    var currentStoryIndex by remember { mutableStateOf(currentIndex) }
    var progress by remember { mutableStateOf(0f) }
    val currentStory = stories.getOrNull(currentStoryIndex)

    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 50),
        label = "progress"
    )

    // Auto-progression
    LaunchedEffect(currentStoryIndex) {
        progress = 0f
        val duration = currentStory?.duration ?: 5000L
        val steps = 100
        val stepDuration = duration / steps

        currentStory?.let { onMarkAsViewed(it) }

        repeat(steps) {
            delay(stepDuration)
            progress = (it + 1) / steps.toFloat()

            if (progress >= 1f && currentStoryIndex < stories.size - 1) {
                currentStoryIndex++
                onStoryChange(currentStoryIndex)
            } else if (progress >= 1f && currentStoryIndex >= stories.size - 1) {
                onClose()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth / 2) {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                                onStoryChange(currentStoryIndex)
                            } else {
                                onClose()
                            }
                        } else {
                            if (currentStoryIndex < stories.size - 1) {
                                currentStoryIndex++
                                onStoryChange(currentStoryIndex)
                            } else {
                                onClose()
                            }
                        }
                    }
                )
            }
    ) {
        currentStory?.let { story ->
            // Media content
            if (story.mediaType == StoryMediaType.IMAGE) {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = "Story",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Gradient background for video placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DarkBackground,
                                        DarkSurface
                                    )
                                )
                            )
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CoralPrimary, VioletPrimary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Vidéo Story",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Top gradient overlay for progress bars and header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.18f)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Progress bars and header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp)
            ) {
                // Gradient progress bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            if (index < currentStoryIndex) {
                                // Completed segments with gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(CoralPrimary, VioletPrimary)
                                            )
                                        )
                                )
                            } else if (index == currentStoryIndex) {
                                // Current segment with animated gradient fill
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(CoralPrimary, VioletPrimary)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Author header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with gradient ring
                    UserAvatar(
                        user = author,
                        size = 40.dp,
                        showStoryRing = false
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = author?.username ?: "Utilisateur",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = formatTimeAgo(story.createdAt),
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // More options button
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Close button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.15f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // Caption and reply bar at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding()
            ) {
                // Caption
                story.caption?.let { caption ->
                    Text(
                        text = caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Reply input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Text field with glass effect
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Envoyer un message...",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Reaction buttons
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = "Envoyer",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val hours = diff / (1000 * 60 * 60)

    return when {
        hours < 1 -> "À l'instant"
        hours < 24 -> "Il y a ${hours}h"
        else -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
