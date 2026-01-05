package com.memoryshare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.Post
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.BottomNavigationBar
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.PostViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: PostViewModel,
    userViewModel: com.memoryshare.app.ui.viewmodel.UserViewModel,
    preferencesViewModel: com.memoryshare.app.ui.viewmodel.PreferencesViewModel,
    currentUser: User?,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStories: () -> Unit = {},
    onNavigateToReels: () -> Unit = {}
) {
    val allPosts by viewModel.posts.collectAsState()
    val followingPosts by viewModel.followingPosts.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    val fabOnLeft by preferencesViewModel.fabOnLeft.collectAsState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Pour vous, 1 = Abonnements

    // Charger les posts des abonnements
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            viewModel.loadFollowingPosts(userId)
        }
    }

    val posts = if (selectedTab == 1) followingPosts else allPosts

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fil d'actualité", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToReels) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = "Reels")
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            onClick = {
                                showOptionsMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = 1,
                onNavigateToMessages = onNavigateToMessages,
                onNavigateToFeed = {},
                onNavigateToMemories = onNavigateToMemories,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Tabs pour basculer entre les modes
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pour vous") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Abonnements") }
                )
            }

            // Barre de Stories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))

                // Bouton "Votre story"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onNavigateToStories() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ajouter story",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Votre story",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "Glissez pour voir les stories →",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Divider()

            Box(modifier = Modifier.fillMaxSize()) {
                // Contenu principal
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = paddingValues.calculateBottomPadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 1) "Aucun abonnement" else "Aucune publication",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedTab == 1) "Suivez des utilisateurs pour voir leurs publications" else "Partagez vos premiers souvenirs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = paddingValues.calculateBottomPadding())
                    ) {
                        items(posts) { post ->
                            val author = allUsers.find { it.id == post.authorId }
                            PostItem(
                                post = post,
                                author = author,
                                currentUser = currentUser,
                                onLikeClick = { viewModel.toggleLike(post) },
                                onCommentClick = { onPostClick(post.id) },
                                onPostClick = { onPostClick(post.id) }
                            )
                            Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }

                // FAB positionné manuellement
                FloatingActionButton(
                    onClick = onCreatePost,
                    modifier = Modifier
                        .align(if (fabOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = 96.dp) // Padding supplémentaire pour éviter la barre de navigation
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle publication")
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    author: User?,
    currentUser: User?,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onPostClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPostClick)
    ) {
        // En-tête de la publication
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(user = author, size = 40.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = author?.username ?: "Utilisateur",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatTime(post.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { /* Plus d'options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Plus")
            }
        }

        // Média
        if (post.mediaUrls.isNotEmpty()) {
            AsyncImage(
                model = post.mediaUrls.first(),
                contentDescription = post.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (post.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "J'aime",
                    tint = if (post.isLikedByCurrentUser) MaterialTheme.colorScheme.error else LocalContentColor.current
                )
            }

            IconButton(onClick = onCommentClick) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Commenter")
            }

            IconButton(onClick = { /* Partager */ }) {
                Icon(Icons.Default.Share, contentDescription = "Partager")
            }
        }

        // Nombre de likes
        if (post.likeCount > 0) {
            Text(
                text = "${post.likeCount} j'aime",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Légende
        if (!post.caption.isNullOrBlank()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${author?.username ?: "Utilisateur"} ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Nombre de commentaires
        if (post.commentCount > 0) {
            Text(
                text = "Voir les ${post.commentCount} commentaires",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable(onClick = onCommentClick)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "Il y a ${days}j"
        hours > 0 -> "Il y a ${hours}h"
        minutes > 0 -> "Il y a ${minutes}min"
        else -> "À l'instant"
    }
}
