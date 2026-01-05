package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.Story
import com.memoryshare.app.data.model.StoryMediaType
import com.memoryshare.app.data.model.User
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

    // Auto-progression
    LaunchedEffect(currentStoryIndex) {
        progress = 0f
        val duration = currentStory?.duration ?: 5000L // 5 secondes par défaut
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
                            // Tap gauche: story précédente
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                                onStoryChange(currentStoryIndex)
                            } else {
                                onClose()
                            }
                        } else {
                            // Tap droit: story suivante
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
            // Média (image ou vidéo)
            if (story.mediaType == StoryMediaType.IMAGE) {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = "Story",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Pour les vidéos, afficher un placeholder pour l'instant
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vidéo Story\n(Player à implémenter)",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            // Overlay supérieur
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                // Barres de progression
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            if (index < currentStoryIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                )
                            } else if (index == currentStoryIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // En-tête avec auteur
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = author?.profilePictureUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = author?.username ?: "Utilisateur",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTimeAgo(story.createdAt),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }
            }

            // Caption en bas
            story.caption?.let { caption ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
