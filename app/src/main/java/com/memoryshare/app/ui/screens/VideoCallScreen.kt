package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.CallViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import kotlinx.coroutines.delay

@Composable
fun VideoCallScreen(
    userId: String,
    navController: NavController,
    callViewModel: CallViewModel,
    userViewModel: UserViewModel
) {
    val user by userViewModel.getUserById(userId).collectAsState(initial = null)
    val isMuted by callViewModel.isMuted.collectAsState()
    val isVideoEnabled by callViewModel.isVideoEnabled.collectAsState()
    val callDuration by callViewModel.callDuration.collectAsState()

    // Timer pour la durée de l'appel
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callViewModel.incrementCallDuration()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Vidéo de l'autre utilisateur (placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!isVideoEnabled) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    user?.let { currentUser ->
                        UserAvatar(
                            user = currentUser,
                            size = 120.dp
                        )
                        Text(
                            text = currentUser.displayName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Placeholder pour la vidéo
                Text(
                    text = "Vidéo de ${user?.displayName ?: "l'utilisateur"}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Miniature de ma vidéo (coin supérieur droit)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(120.dp, 160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!isVideoEnabled) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = "Caméra désactivée",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Text(
                    text = "Ma vidéo",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Informations de l'appel (en haut)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatCallDuration(callDuration),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        // Contrôles de l'appel (en bas)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Boutons de contrôle
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bouton Muet
                VideoCallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Activer le micro" else "Couper le micro",
                    isActive = isMuted,
                    onClick = { callViewModel.toggleMute() }
                )

                // Bouton Caméra
                VideoCallControlButton(
                    icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (isVideoEnabled) "Désactiver la caméra" else "Activer la caméra",
                    isActive = !isVideoEnabled,
                    onClick = { callViewModel.toggleVideo() }
                )

                // Bouton Changer de caméra
                VideoCallControlButton(
                    icon = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Changer de caméra",
                    onClick = { /* TODO: Implémenter le changement de caméra */ }
                )
            }

            // Bouton Raccrocher
            FloatingActionButton(
                onClick = {
                    callViewModel.endCall()
                    navController.popBackStack()
                },
                containerColor = Color.Red,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Raccrocher",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun VideoCallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.error
                else Color.White.copy(alpha = 0.3f)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun formatCallDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
