package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.Post
import com.memoryshare.app.data.model.PostMediaType
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.BottomNavigationBar
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.components.VideoPlayer
import com.memoryshare.app.ui.theme.*
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
    onNavigateToReels: () -> Unit = {},
    onMediaClick: (String) -> Unit = {}
) {
    val allPosts by viewModel.posts.collectAsState()
    val followingPosts by viewModel.followingPosts.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    val fabOnLeft by preferencesViewModel.fabOnLeft.collectAsState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            viewModel.loadFeedPosts(userId)
            viewModel.loadFollowingPosts(userId)
        }
    }

    val posts = if (selectedTab == 1) followingPosts else allPosts

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Actualités",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToReels) {
                        Icon(
                            Icons.Outlined.VideoLibrary,
                            contentDescription = "Reels",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            onClick = {
                                showOptionsMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom gradient tabs
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = paddingValues.calculateTopPadding()),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChipTab(
                        text = "Pour vous",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipTab(
                        text = "Abonnements",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Stories bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onNavigateToStories() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                CoralPrimary.copy(alpha = 0.15f),
                                                VioletPrimary.copy(alpha = 0.15f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Ajouter story",
                                    modifier = Modifier.size(28.dp),
                                    tint = CoralPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Votre story",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Glissez pour voir les stories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Feed content
            Box(modifier = Modifier.fillMaxSize()) {
                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = paddingValues.calculateBottomPadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                CoralPrimary.copy(alpha = 0.15f),
                                                VioletPrimary.copy(alpha = 0.15f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = CoralPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (selectedTab == 1) "Aucun abonnement" else "Aucune publication",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
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
                            .padding(bottom = paddingValues.calculateBottomPadding()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(posts) { post ->
                            val author = allUsers.find { it.id == post.authorId }
                            PostItem(
                                post = post,
                                author = author,
                                currentUser = currentUser,
                                onLikeClick = { viewModel.toggleLike(post) },
                                onCommentClick = { onPostClick(post.id) },
                                onPostClick = { onPostClick(post.id) },
                                onFollowClick = { userViewModel.toggleFollow(post.authorId) },
                                onMediaClick = { onMediaClick(post.id) },
                                onEditCaption = { caption -> viewModel.updatePostCaption(post, caption) },
                                onChangeVisibility = { visibility -> viewModel.updatePostVisibility(post, visibility) },
                                onDeletePost = { viewModel.deletePost(post) }
                            )
                        }
                    }
                }

                // Gradient FAB
                Box(
                    modifier = Modifier
                        .align(if (fabOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                        .padding(20.dp)
                        .padding(bottom = 96.dp)
                ) {
                    FloatingActionButton(
                        onClick = onCreatePost,
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CoralPrimary, VioletPrimary)
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Nouvelle publication",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(19.dp),
        color = if (selected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selected) Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(CoralPrimary, VioletPrimary)
                        ),
                        shape = RoundedCornerShape(19.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    onPostClick: () -> Unit,
    onFollowClick: () -> Unit = {},
    onMediaClick: () -> Unit = {},
    onEditCaption: (String) -> Unit = {},
    onChangeVisibility: (com.memoryshare.app.data.model.PostVisibility) -> Unit = {},
    onDeletePost: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditCaptionDialog by remember { mutableStateOf(false) }
    var showChangeVisibilityDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val isAuthor = post.authorId == currentUser?.id

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onPostClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column {
            // Post header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(user = author, size = 40.dp, showStoryRing = true)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = author?.displayName ?: author?.username ?: "Utilisateur",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Follow button for other users' posts
                        if (!isAuthor) {
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(CoralPrimary, VioletPrimary)
                                        )
                                    )
                                    .clickable { onFollowClick() }
                                    .padding(horizontal = 10.dp),
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
                    }
                    Text(
                        text = formatFeedTime(post.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Outlined.MoreHoriz,
                            contentDescription = "Plus",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isAuthor) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Modifier la légende") },
                                onClick = {
                                    showMenu = false
                                    showEditCaptionDialog = true
                                },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Changer la visibilité") },
                                onClick = {
                                    showMenu = false
                                    showChangeVisibilityDialog = true
                                },
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer", color = ErrorRose) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = ErrorRose) }
                            )
                        }
                    }
                }
            }

            // Media - with video player support
            if (post.mediaUrls.isNotEmpty()) {
                if (post.mediaType == PostMediaType.VIDEO) {
                    VideoPlayer(
                        videoUrl = post.mediaUrls.first(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        autoPlay = false,
                        showControls = true,
                        onVideoClick = { onMediaClick() }
                    )
                } else {
                    AsyncImage(
                        model = post.mediaUrls.first(),
                        contentDescription = post.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clickable { onMediaClick() },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Actions row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "J'aime",
                        tint = if (post.isLikedByCurrentUser) LikeRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onCommentClick) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Commenter",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Partager",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { }) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = "Sauvegarder",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Likes count
            if (post.likeCount > 0) {
                Text(
                    text = "${post.likeCount} j'aime",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Caption
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

            // Comments count
            if (post.commentCount > 0) {
                Text(
                    text = "Voir les ${post.commentCount} commentaires",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable(onClick = onCommentClick)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Edit caption dialog
    if (showEditCaptionDialog) {
        var editedCaption by remember { mutableStateOf(post.caption ?: "") }
        AlertDialog(
            onDismissRequest = { showEditCaptionDialog = false },
            title = { Text("Modifier la légende", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedCaption,
                    onValueChange = { editedCaption = it },
                    placeholder = { Text("Entrez une légende") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditCaption(editedCaption)
                        showEditCaptionDialog = false
                    }
                ) {
                    Text("Enregistrer", color = CoralPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCaptionDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Visibility dialog
    if (showChangeVisibilityDialog) {
        var selectedVisibility by remember { mutableStateOf(post.visibility) }
        AlertDialog(
            onDismissRequest = { showChangeVisibilityDialog = false },
            title = { Text("Changer la visibilité", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    com.memoryshare.app.data.model.PostVisibility.values().forEach { visibility ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedVisibility = visibility }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVisibility == visibility,
                                onClick = { selectedVisibility = visibility },
                                colors = RadioButtonDefaults.colors(selectedColor = CoralPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (visibility) {
                                    com.memoryshare.app.data.model.PostVisibility.PUBLIC -> "Public"
                                    com.memoryshare.app.data.model.PostVisibility.FRIENDS -> "Amis uniquement"
                                    com.memoryshare.app.data.model.PostVisibility.FOLLOWERS -> "Followers uniquement"
                                    com.memoryshare.app.data.model.PostVisibility.FRIENDS_AND_FOLLOWERS -> "Amis et followers"
                                    com.memoryshare.app.data.model.PostVisibility.PRIVATE -> "Privé"
                                }
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        onChangeVisibility(selectedVisibility)
                        showChangeVisibilityDialog = false
                    }
                ) {
                    Text("Enregistrer", color = CoralPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeVisibilityDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Delete confirm dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer la publication", fontWeight = FontWeight.Bold) },
            text = { Text("Cette action est irréversible. Voulez-vous continuer ?") },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePost()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Supprimer", color = ErrorRose, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

private fun formatFeedTime(timestamp: Long): String {
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
