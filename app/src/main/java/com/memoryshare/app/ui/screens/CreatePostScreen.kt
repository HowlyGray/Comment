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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.PostMediaType
import com.memoryshare.app.data.model.StoryMediaType
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.StoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    currentUser: User?,
    onBack: () -> Unit,
    onPostCreated: () -> Unit
) {
    var selectedMediaType by remember { mutableStateOf(PostMediaType.IMAGE) }
    var caption by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaUrl by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showMediaSourceDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var publishAsStory by remember { mutableStateOf(false) }

    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val uploadError by viewModel.uploadError.collectAsStateWithLifecycle()

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
                title = { Text("Nouvelle publication") },
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
                                errorMessage = null

                                // Créer le post
                                viewModel.createPostWithMedia(
                                    authorId = currentUser.id,
                                    mediaUri = mediaUri,
                                    mediaUrl = mediaUrl,
                                    mediaType = selectedMediaType,
                                    caption = caption.ifBlank { null },
                                    onSuccess = { uploadedUrl ->
                                        // Si l'utilisateur veut aussi publier en story
                                        if (publishAsStory && (selectedMediaType == PostMediaType.IMAGE || selectedMediaType == PostMediaType.VIDEO)) {
                                            // Convertir le type de média
                                            val storyMediaType = when (selectedMediaType) {
                                                PostMediaType.IMAGE -> StoryMediaType.IMAGE
                                                PostMediaType.VIDEO -> StoryMediaType.VIDEO
                                                else -> StoryMediaType.IMAGE // Fallback (ne devrait pas arriver)
                                            }

                                            // Créer la story avec la même URL uploadée
                                            storyViewModel.createStory(
                                                authorId = currentUser.id,
                                                mediaUrl = uploadedUrl,
                                                mediaType = storyMediaType,
                                                caption = caption.ifBlank { null }
                                            )
                                        }

                                        isLoading = false
                                        onPostCreated()
                                    },
                                    onError = { error ->
                                        isLoading = false
                                        errorMessage = error
                                    }
                                )
                            }
                        },
                        enabled = mediaUrl.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Publier")
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
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMediaType == PostMediaType.IMAGE,
                            onClick = { selectedMediaType = PostMediaType.IMAGE },
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
                            selected = selectedMediaType == PostMediaType.VIDEO,
                            onClick = { selectedMediaType = PostMediaType.VIDEO },
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
                            selected = selectedMediaType == PostMediaType.AUDIO,
                            onClick = { selectedMediaType = PostMediaType.AUDIO },
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

            // Zone de prévisualisation/sélection
            if (mediaUrl.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (selectedMediaType) {
                                PostMediaType.IMAGE -> Icons.Default.Image
                                PostMediaType.VIDEO -> Icons.Default.VideoLibrary
                                PostMediaType.AUDIO -> Icons.Default.AudioFile
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showMediaSourceDialog = true }) {
                            Text("Ajouter ${when (selectedMediaType) {
                                PostMediaType.IMAGE -> "une image"
                                PostMediaType.VIDEO -> "une vidéo"
                                PostMediaType.AUDIO -> "un audio"
                            }}")
                        }
                    }
                }
            } else {
                // Prévisualisation pour les images
                if (selectedMediaType == PostMediaType.IMAGE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = "Image sélectionnée",
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
                            .height(300.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (selectedMediaType) {
                                    PostMediaType.VIDEO -> Icons.Default.VideoLibrary
                                    PostMediaType.AUDIO -> Icons.Default.AudioFile
                                    else -> Icons.Default.Image
                                },
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (selectedMediaType) {
                                    PostMediaType.VIDEO -> "Vidéo ajoutée"
                                    PostMediaType.AUDIO -> "Audio ajouté"
                                    else -> "Média ajouté"
                                },
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

            // Champ de légende
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Écrivez une légende...") },
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Option pour publier en story (uniquement pour images et vidéos)
            if (selectedMediaType == PostMediaType.IMAGE || selectedMediaType == PostMediaType.VIDEO) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = publishAsStory,
                            onCheckedChange = { publishAsStory = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Publier en story également",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Cette publication apparaîtra dans votre fil et comme story (24h)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Instructions
            if (mediaUrl.isBlank()) {
                Text(
                    text = "Ajoutez ${when (selectedMediaType) {
                        PostMediaType.IMAGE -> "une image"
                        PostMediaType.VIDEO -> "une vidéo"
                        PostMediaType.AUDIO -> "un audio"
                    }} pour continuer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Affichage de la progression de l'upload
            if (isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uploadProgress != null) {
                            Text(
                                text = "Upload en cours...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = uploadProgress ?: 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Préparation de la publication...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Affichage des erreurs
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            uploadError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
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
                            PostMediaType.IMAGE -> "image"
                            PostMediaType.VIDEO -> "vidéo"
                            PostMediaType.AUDIO -> "audio"
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
                                PostMediaType.IMAGE -> {
                                    visualMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                PostMediaType.VIDEO -> {
                                    visualMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                                PostMediaType.AUDIO -> {
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

    // Dialog pour entrer l'URL du média
    if (showUrlDialog) {
        var tempUrl by remember { mutableStateOf(mediaUrl) }

        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("URL du média") },
            text = {
                Column {
                    Text(
                        text = "Entrez l'URL du ${when (selectedMediaType) {
                            PostMediaType.IMAGE -> "image"
                            PostMediaType.VIDEO -> "vidéo"
                            PostMediaType.AUDIO -> "fichier audio"
                        }}:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        placeholder = { Text(when (selectedMediaType) {
                            PostMediaType.IMAGE -> "https://exemple.com/photo.jpg"
                            PostMediaType.VIDEO -> "https://exemple.com/video.mp4"
                            PostMediaType.AUDIO -> "https://exemple.com/audio.mp3"
                        }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (selectedMediaType) {
                            PostMediaType.IMAGE -> "Exemples d'images:\n• https://picsum.photos/800/600\n• https://source.unsplash.com/random/800x600"
                            PostMediaType.VIDEO -> "Exemples de vidéos:\n• https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                            PostMediaType.AUDIO -> "Exemples d'audio:\n• https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
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
