package com.memoryshare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.theme.*

@Composable
fun UserAvatar(
    user: User?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    showStoryRing: Boolean = false,
    isOnline: Boolean = false
) {
    val storyGradient = Brush.sweepGradient(
        colors = listOf(
            CoralPrimary,
            GradientSunset,
            VioletPrimary,
            GradientSunset,
            CoralPrimary
        )
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Story ring (gradient border)
        if (showStoryRing) {
            Box(
                modifier = Modifier
                    .size(size + 6.dp)
                    .clip(CircleShape)
                    .background(storyGradient)
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarContent(user = user, size = size)
            }
        } else {
            AvatarContent(user = user, size = size)
        }

        // Online indicator
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
            )
        }
    }
}

@Composable
private fun AvatarContent(
    user: User?,
    size: Dp
) {
    if (user?.profilePictureUrl != null) {
        AsyncImage(
            model = user.profilePictureUrl,
            contentDescription = "Photo de profil",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Gradient fallback avatar with initial
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            CoralPrimary.copy(alpha = 0.7f),
                            VioletPrimary.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user?.displayName?.firstOrNull()?.uppercase() ?: "?",
                style = when {
                    size >= 80.dp -> MaterialTheme.typography.headlineMedium
                    size >= 48.dp -> MaterialTheme.typography.titleLarge
                    size >= 32.dp -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.labelMedium
                },
                color = Color.White
            )
        }
    }
}
