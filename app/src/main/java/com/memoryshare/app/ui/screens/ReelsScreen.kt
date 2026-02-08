package com.memoryshare.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.Reel
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.theme.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    reels: List<Reel>,
    currentIndex: Int,
    allUsers: List<User>,
    onReelChange: (Int) -> Unit,
    onLikeClick: (Reel) -> Unit,
    onCommentClick: (Reel) -> Unit,
    onShareClick: (Reel) -> Unit,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { reels.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onReelChange(pagerState.currentPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val reel = reels[page]
            val author = allUsers.find { it.id == reel.authorId }

            ReelItem(
                reel = reel,
                author = author,
                onLikeClick = { onLikeClick(reel) },
                onCommentClick = { onCommentClick(reel) },
                onShareClick = { onShareClick(reel) }
            )
        }

        // Premium back button with blur effect
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp)
                .shadow(8.dp, CircleShape)
                .background(
                    Color.Black.copy(alpha = 0.4f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Retour",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // "Reels" title
        Text(
            text = "Reels",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp)
        )
    }
}

@Composable
fun ReelItem(
    reel: Reel,
    author: User?,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    // Animated music disc rotation
    val infiniteTransition = rememberInfiniteTransition(label = "disc")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "discRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video / Thumbnail
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = reel.thumbnailUrl ?: reel.videoUrl,
                contentDescription = "Reel",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Gradient play button overlay
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Right side action buttons with premium styling
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Author avatar with gradient ring
            Box(contentAlignment = Alignment.BottomCenter) {
                UserAvatar(
                    user = author,
                    size = 48.dp,
                    showStoryRing = true
                )
                // Follow badge
                Box(
                    modifier = Modifier
                        .offset(y = 8.dp)
                        .size(20.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CoralPrimary, VioletPrimary)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Follow",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Like button
            ReelActionButton(
                icon = if (reel.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                count = reel.likeCount,
                onClick = onLikeClick,
                tint = if (reel.isLikedByCurrentUser) LikeRed else Color.White
            )

            // Comments button
            ReelActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                count = reel.commentCount,
                onClick = onCommentClick,
                tint = Color.White
            )

            // Share button
            ReelActionButton(
                icon = Icons.Outlined.Send,
                count = reel.shareCount,
                onClick = onShareClick,
                tint = Color.White
            )

            // Bookmark button
            IconButton(
                onClick = { },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    contentDescription = "Enregistrer",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Spinning music disc
            reel.audioName?.let {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .rotate(discRotation)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    CoralPrimary.copy(alpha = 0.8f),
                                    VioletPrimary.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Audio",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Bottom info section
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.75f)
                .padding(16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Author name with follow button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = author?.username ?: "Utilisateur",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                // Gradient follow button
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CoralPrimary, VioletPrimary)
                            )
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Suivre",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            reel.caption?.let { caption ->
                Text(
                    text = caption,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Audio info with animated marquee feel
            reel.audioName?.let { audioName ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = audioName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    onClick: () -> Unit,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        if (count > 0) {
            Text(
                text = formatCount(count),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
