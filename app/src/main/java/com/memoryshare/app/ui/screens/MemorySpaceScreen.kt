package com.memoryshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.MediaItem
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySpaceScreen(
    spaceId: String,
    viewModel: SharedSpaceViewModel,
    currentUser: User?,
    onBack: () -> Unit,
    onAddMedia: () -> Unit
) {
    val space by viewModel.currentSpace.collectAsState()
    val media by viewModel.media.collectAsState()
    var selectedFilter by remember { mutableStateOf<MediaType?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(spaceId, selectedFilter) {
        viewModel.loadSpace(spaceId)
        viewModel.loadMedia(spaceId, selectedFilter)
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

                    IconButton(onClick = { /* Gérer les membres */ }) {
                        Icon(Icons.Default.Group, contentDescription = "Membres")
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
}
