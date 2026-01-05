package com.memoryshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.PermissionLevel
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.MediaItem
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySpaceScreen(
    spaceId: String,
    viewModel: SharedSpaceViewModel,
    userViewModel: UserViewModel,
    currentUser: User?,
    onBack: () -> Unit,
    onAddMedia: () -> Unit
) {
    val space by viewModel.currentSpace.collectAsState()
    val media by viewModel.media.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    var selectedFilter by remember { mutableStateOf<MediaType?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(spaceId, selectedFilter) {
        viewModel.loadSpace(spaceId)
        viewModel.loadMedia(spaceId, selectedFilter)
        viewModel.loadPermissions(spaceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = space?.name ?: "Espace partagé",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = if (selectedFilter != null) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            contentDescription = "Filtrer"
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tous les médias") },
                            onClick = {
                                selectedFilter = null
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Photos") },
                            onClick = {
                                selectedFilter = MediaType.IMAGE
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Vidéos") },
                            onClick = {
                                selectedFilter = MediaType.VIDEO
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Audio") },
                            onClick = {
                                selectedFilter = MediaType.AUDIO
                                showFilterMenu = false
                            }
                        )
                    }

                    IconButton(onClick = { showPermissionsDialog = true }) {
                        Icon(Icons.Default.Group, contentDescription = "Gérer les permissions")
                    }

                    IconButton(onClick = { /* Plus d'options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Plus")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedia
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un média")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Informations sur l'espace
            space?.let { currentSpace ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (currentSpace.description != null) {
                            Text(
                                text = currentSpace.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentSpace.mediaCount} médias",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentSpace.memberIds.size} membres",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Grille de médias
            if (media.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
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
                            text = "Aucun média",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ajoutez vos premiers souvenirs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(media) { mediaItem ->
                        MediaItem(
                            media = mediaItem,
                            onClick = { /* Ouvrir le média en plein écran */ }
                        )
                    }
                }
            }
        }
    }

    // Dialog de gestion des permissions
    if (showPermissionsDialog) {
        PermissionsDialog(
            space = space,
            permissions = permissions,
            allUsers = allUsers,
            currentUser = currentUser,
            onDismiss = { showPermissionsDialog = false },
            onPermissionChange = { userId, permissionLevel ->
                viewModel.updatePermission(spaceId, userId, permissionLevel)
            },
            onRemoveMember = { userId ->
                viewModel.removeMember(spaceId, userId)
            }
        )
    }
}

@Composable
fun PermissionsDialog(
    space: com.memoryshare.app.data.model.SharedSpace?,
    permissions: List<com.memoryshare.app.data.model.SharedSpacePermission>,
    allUsers: List<User>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onPermissionChange: (String, PermissionLevel) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Gérer les permissions", fontWeight = FontWeight.Bold)
                space?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (space != null) {
                    items(space.memberIds) { userId ->
                        val user = allUsers.find { it.id == userId }
                        val permission = permissions.find { it.userId == userId }
                        val isOwner = space.createdBy == userId
                        val isCurrentUser = currentUser?.id == userId

                        MemberPermissionItem(
                            user = user,
                            permissionLevel = permission?.permissionLevel ?: PermissionLevel.READ,
                            isOwner = isOwner,
                            isCurrentUser = isCurrentUser,
                            onPermissionChange = { newLevel ->
                                onPermissionChange(userId, newLevel)
                            },
                            onRemove = {
                                if (!isOwner && !isCurrentUser) {
                                    onRemoveMember(userId)
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    // Aide sur les permissions
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Niveaux de permission",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• LECTURE : Peut voir les médias\n• ÉCRITURE : Peut ajouter et supprimer des médias",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun MemberPermissionItem(
    user: User?,
    permissionLevel: PermissionLevel,
    isOwner: Boolean,
    isCurrentUser: Boolean,
    onPermissionChange: (PermissionLevel) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(user = user, size = 40.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user?.displayName ?: "Utilisateur",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isOwner) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Propriétaire",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(Vous)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sélecteur de permission
                if (!isOwner) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = permissionLevel == PermissionLevel.READ,
                            onClick = { onPermissionChange(PermissionLevel.READ) },
                            label = { Text("LECTURE", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        FilterChip(
                            selected = permissionLevel == PermissionLevel.WRITE,
                            onClick = { onPermissionChange(PermissionLevel.WRITE) },
                            label = { Text("ÉCRITURE", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Contrôle total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Bouton supprimer (pas pour le propriétaire ni pour soi-même)
            if (!isOwner && !isCurrentUser) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Retirer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
