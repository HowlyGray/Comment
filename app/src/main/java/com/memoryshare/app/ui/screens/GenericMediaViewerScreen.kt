package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.memoryshare.app.ui.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericMediaViewerScreen(
    mediaUrl: String,
    mediaType: String, // "IMAGE", "VIDEO", "AUDIO"
    title: String? = null,
    description: String? = null,
    onBack: () -> Unit,
    onShare: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: "Média") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Partager")
                    }
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Télécharger")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (mediaType.uppercase()) {
                "IMAGE" -> {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = description,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            },
                        contentScale = ContentScale.Fit
                    )
                }
                "VIDEO" -> {
                    VideoPlayer(
                        videoUrl = mediaUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = true,
                        showControls = true
                    )
                }
                "AUDIO" -> {
                    // TODO: Implémenter le lecteur audio
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Lecteur audio à implémenter", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(mediaUrl, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Description/caption overlay
            if (!description.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = description,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
