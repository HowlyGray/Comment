package com.memoryshare.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaScreen(
    spaceId: String,
    viewModel: SharedSpaceViewModel,
    currentUser: User?,
    onBack: () -> Unit,
    onMediaAdded: () -> Unit
) {
    var selectedMediaType by remember { mutableStateOf(MediaType.IMAGE) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showMediaSourceDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Launcher pour sélectionner une image ou vidéo
    val visualMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            mediaUri = it
            mediaUrl = it.toString()
        }
    }

    // Launcher pour sélectionner un fichier audio
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            mediaUri = it
            mediaUrl = it.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un média") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentUser != null && mediaUrl.isNotBlank()) {
                                isLoading = true
                                viewModel.addMedia(
                                    spaceId = spaceId,
                                    uploaderId = currentUser.id,
                                    url = mediaUrl,
                                    type = selectedMediaType,
                                    title = title.ifBlank { null },
                                    description = description.ifBlank { null }
                                )
                                onMediaAdded()
                            }
                        },
                        enabled = mediaUrl.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Ajouter")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Sélection du type de média
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Type de média",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMediaType == MediaType.IMAGE,
                            onClick = { selectedMediaType = MediaType.IMAGE },
                            label = { Text("Photo") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        FilterChip(
                            selected = selectedMediaType == MediaType.VIDEO,
                            onClick = { selectedMediaType = MediaType.VIDEO },
                            label = { Text("Vidéo") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        FilterChip(
                            selected = selectedMediaType == MediaType.AUDIO,
                            onClick = { selectedMediaType = MediaType.AUDIO },
                            label = { Text("Audio") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Zone de prévisualisation
            if (mediaUrl.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (selectedMediaType) {
                                MediaType.IMAGE -> Icons.Default.Image
                                MediaType.VIDEO -> Icons.Default.VideoLibrary
                                MediaType.AUDIO -> Icons.Default.AudioFile
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showMediaSourceDialog = true }) {
                            Text("Ajouter ${when (selectedMediaType) {
                                MediaType.IMAGE -> "une image"
                                MediaType.VIDEO -> "une vidéo"
                                MediaType.AUDIO -> "un audio"
                            }}")
                        }
                    }
                }
            } else {
                // Prévisualisation pour les images
                if (selectedMediaType == MediaType.IMAGE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = "Aperçu",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        TextButton(
                            onClick = { showMediaSourceDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text("Changer", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                } else {
                    // Pour vidéo et audio, afficher un placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (selectedMediaType) {
                                    MediaType.VIDEO -> Icons.Default.VideoLibrary
                                    MediaType.AUDIO -> Icons.Default.AudioFile
                                    else -> Icons.Default.Image
                                },
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "URL ajoutée",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showMediaSourceDialog = true }) {
                                Text("Changer")
                            }
                        }
                    }
                }
            }

            Divider()

            // Champs de saisie
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Titre (optionnel)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (optionnel)") },
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions
                if (mediaUrl.isBlank()) {
                    Text(
                        text = "Ajoutez ${when (selectedMediaType) {
                            MediaType.IMAGE -> "une image"
                            MediaType.VIDEO -> "une vidéo"
                            MediaType.AUDIO -> "un fichier audio"
                        }} pour continuer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Dialog pour choisir la source du média
    if (showMediaSourceDialog) {
        AlertDialog(
            onDismissRequest = { showMediaSourceDialog = false },
            title = { Text("Choisir la source") },
            text = {
                Column {
                    Text(
                        text = "Comment souhaitez-vous ajouter votre ${when (selectedMediaType) {
                            MediaType.IMAGE -> "image"
                            MediaType.VIDEO -> "vidéo"
                            MediaType.AUDIO -> "audio"
                        }} ?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showMediaSourceDialog = false
                            when (selectedMediaType) {
                                MediaType.IMAGE -> {
                                    visualMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                MediaType.VIDEO -> {
                                    visualMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                                MediaType.AUDIO -> {
                                    audioPickerLauncher.launch("audio/*")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fichier local")
                    }

                    Button(
                        onClick = {
                            showMediaSourceDialog = false
                            showUrlDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("URL")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMediaSourceDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog pour entrer l'URL
    if (showUrlDialog) {
        var tempUrl by remember { mutableStateOf(mediaUrl) }

        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("URL du média") },
            text = {
                Column {
                    Text(
                        text = "Entrez l'URL du ${when (selectedMediaType) {
                            MediaType.IMAGE -> "image"
                            MediaType.VIDEO -> "vidéo"
                            MediaType.AUDIO -> "fichier audio"
                        }}:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        placeholder = { Text(when (selectedMediaType) {
                            MediaType.IMAGE -> "https://exemple.com/photo.jpg"
                            MediaType.VIDEO -> "https://exemple.com/video.mp4"
                            MediaType.AUDIO -> "https://exemple.com/audio.mp3"
                        }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (selectedMediaType) {
                            MediaType.IMAGE -> "Exemples d'images:\n• https://picsum.photos/800/600\n• https://source.unsplash.com/random/800x600"
                            MediaType.VIDEO -> "Exemples de vidéos:\n• https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                            MediaType.AUDIO -> "Exemples d'audio:\n• https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mediaUrl = tempUrl
                        mediaUri = null
                        showUrlDialog = false
                    },
                    enabled = tempUrl.isNotBlank()
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
