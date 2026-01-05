package com.memoryshare.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.Reel
import com.memoryshare.app.data.model.User

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

        // Bouton retour en haut
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Retour",
                tint = Color.White
            )
        }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Vidéo (placeholder pour l'instant)
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
            Text(
                text = "Vidéo Reel\n(Player ExoPlayer à ajouter)",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Actions à droite
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Avatar auteur
            AsyncImage(
                model = author?.profilePictureUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )

            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        if (reel.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (reel.isLikedByCurrentUser) Color.Red else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                if (reel.likeCount > 0) {
                    Text(
                        text = formatCount(reel.likeCount),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Commentaires
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onCommentClick) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = "Commentaires",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                if (reel.commentCount > 0) {
                    Text(
                        text = formatCount(reel.commentCount),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Partage
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onShareClick) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Partager",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                if (reel.shareCount > 0) {
                    Text(
                        text = formatCount(reel.shareCount),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Audio
            reel.audioName?.let { audioName ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Audio",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Informations en bas
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.7f)
                .padding(16.dp)
        ) {
            // Auteur
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author?.username ?: "Utilisateur",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { /* TODO: Follow */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Suivre", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            reel.caption?.let { caption ->
                Text(
                    text = caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Audio info
            reel.audioName?.let { audioName ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = audioName,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
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
