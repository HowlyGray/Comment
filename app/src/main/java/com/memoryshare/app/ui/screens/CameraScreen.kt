package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun CameraScreen(
    navController: NavController,
    onPhotoCaptured: (String) -> Unit = {},
    onVideoCaptured: (String) -> Unit = {}
) {
    var isVideoMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Placeholder pour l'aperçu de la caméra
        // Note: L'implémentation complète nécessiterait CameraX
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aperçu de la caméra\n(Nécessite CameraX)",
                color = Color.White
            )
        }

        // Contrôles en haut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Bouton Fermer
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = Color.White
                )
            }

            // Bouton Flash
            IconButton(
                onClick = { /* TODO: Toggle flash */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }

        // Contrôles en bas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sélecteur Photo/Vidéo
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isVideoMode = false }
                ) {
                    Text(
                        text = "PHOTO",
                        color = if (!isVideoMode) Color.White else Color.Gray
                    )
                }
                TextButton(
                    onClick = { isVideoMode = true }
                ) {
                    Text(
                        text = "VIDÉO",
                        color = if (isVideoMode) Color.White else Color.Gray
                    )
                }
            }

            // Contrôles de capture
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bouton Galerie
                IconButton(
                    onClick = { /* TODO: Ouvrir galerie */ },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Galerie",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bouton Capture
                FloatingActionButton(
                    onClick = {
                        if (isVideoMode) {
                            isRecording = !isRecording
                            if (!isRecording) {
                                // Vidéo capturée
                                onVideoCaptured("video_path")
                            }
                        } else {
                            // Photo capturée
                            onPhotoCaptured("photo_path")
                        }
                    },
                    containerColor = if (isRecording) Color.Red else Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    if (isVideoMode && isRecording) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, shape = MaterialTheme.shapes.small)
                        )
                    } else {
                        Icon(
                            imageVector = if (isVideoMode) Icons.Default.FiberManualRecord
                                         else Icons.Default.Camera,
                            contentDescription = if (isVideoMode) "Enregistrer" else "Capturer",
                            tint = if (isRecording) Color.White else Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bouton Changer de caméra
                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Changer de caméra",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Indicateur d'enregistrement
            if (isRecording) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text(
                        text = "Enregistrement en cours...",
                        color = Color.White
                    )
                }
            }
        }
    }
}
