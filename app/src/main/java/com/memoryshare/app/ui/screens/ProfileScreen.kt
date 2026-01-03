package com.memoryshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.BottomNavigationBar
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: UserViewModel,
    currentUser: User?,
    onNavigateToMessages: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentUser?.username ?: "username",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
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
                selectedTab = 3,
                onNavigateToMessages = onNavigateToMessages,
                onNavigateToFeed = onNavigateToFeed,
                onNavigateToMemories = onNavigateToMemories,
                onNavigateToProfile = {}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // En-tête du profil - style Instagram
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Photo de profil
                UserAvatar(
                    user = currentUser,
                    size = 88.dp
                )

                // Statistiques
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(
                        count = "0",
                        label = "Publications"
                    )
                    ProfileStat(
                        count = "0",
                        label = "Abonnés"
                    )
                    ProfileStat(
                        count = "0",
                        label = "Abonnements"
                    )
                }
            }

            // Nom et bio
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = currentUser?.displayName ?: "Nom d'affichage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (currentUser?.bio != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentUser.bio,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Boutons d'action - style Instagram
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Modifier le profil")
                }

                OutlinedButton(
                    onClick = { /* Partager le profil */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Partager le profil")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs - Publications (pour l'instant juste l'onglet grille)
            TabRow(
                selectedTabIndex = 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = true,
                    onClick = { },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Publications"
                        )
                    }
                )
            }

            // Grille de publications (vide pour l'instant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune publication",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Partagez des photos et des vidéos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentUser = currentUser,
            onDismiss = { showEditDialog = false },
            onSave = { displayName, bio ->
                if (currentUser != null) {
                    viewModel.updateUser(
                        currentUser.copy(
                            displayName = displayName,
                            bio = bio
                        )
                    )
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ProfileStat(
    count: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentUser: User?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le profil") },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nom d'affichage") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biographie") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (displayName.isNotBlank()) {
                        onSave(displayName, bio.ifBlank { null })
                    }
                },
                enabled = displayName.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
