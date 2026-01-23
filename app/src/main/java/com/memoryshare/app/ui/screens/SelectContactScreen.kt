package com.memoryshare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectContactScreen(
    userViewModel: UserViewModel,
    currentUser: User?,
    onContactSelected: (User) -> Unit,
    onCreateGroup: () -> Unit,
    onAddNewContact: () -> Unit,
    onBack: () -> Unit
) {
    val users by userViewModel.users.collectAsState()
    val searchResults by userViewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Filtrer les utilisateurs pour exclure l'utilisateur actuel
    val availableUsers = if (searchQuery.isBlank()) {
        users.filter { it.id != currentUser?.id }
    } else {
        searchResults.filter { it.id != currentUser?.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sélectionner un contact", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Rechercher */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Rechercher")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Barre de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    if (query.isNotBlank()) {
                        userViewModel.searchUsers(query)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Rechercher un contact...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Options principales
                if (searchQuery.isBlank()) {
                    item {
                        ContactOption(
                            icon = Icons.Default.Group,
                            title = "Nouveau groupe",
                            subtitle = "Créer un groupe avec plusieurs contacts",
                            onClick = onCreateGroup
                        )
                        Divider()
                    }

                    item {
                        ContactOption(
                            icon = Icons.Default.PersonAdd,
                            title = "Nouveau contact",
                            subtitle = "Ajouter un nouveau contact",
                            onClick = onAddNewContact
                        )
                        Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }

                    item {
                        Text(
                            text = "CONTACTS SUR MEMORYSHARE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Liste des contacts
                if (availableUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PersonOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "Aucun contact" else "Aucun résultat",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(availableUsers) { user ->
                        ContactItem(
                            user = user,
                            onClick = { onContactSelected(user) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun ContactOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun ContactItem(
    user: User,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = user.displayName,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = user.bio ?: "Disponible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            UserAvatar(user = user, size = 40.dp)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
