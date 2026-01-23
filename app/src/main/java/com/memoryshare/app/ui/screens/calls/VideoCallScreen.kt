package com.memoryshare.app.ui.screens.calls

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.memoryshare.app.MemoryShareApplication
import com.memoryshare.app.services.calls.AgoraManager
import kotlinx.coroutines.delay

/**
 * Video Call Screen
 * Full-screen video call interface with Agora SDK
 */
@Composable
fun VideoCallScreen(
    channelName: String,
    callerName: String,
    isVideo: Boolean = true,
    token: String? = null,
    onCallEnd: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MemoryShareApplication
    val agoraManager = remember { app.agoraManager }

    val callState by agoraManager.callState.collectAsState()
    val isMuted by agoraManager.isMuted.collectAsState()
    val isSpeakerOn by agoraManager.isSpeakerOn.collectAsState()
    val isVideoEnabled by agoraManager.isVideoEnabled.collectAsState()
    val isFrontCamera by agoraManager.isFrontCamera.collectAsState()
    val remoteUsers by agoraManager.remoteUsers.collectAsState()
    val callDuration by agoraManager.callDuration.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && callState is AgoraManager.CallState.Connected) {
            delay(5000)
            showControls = false
        }
    }

    // Join call on launch
    LaunchedEffect(Unit) {
        agoraManager.initialize()
        if (isVideo) {
            agoraManager.joinVideoCall(channelName, token)
        } else {
            agoraManager.joinVoiceCall(channelName, token)
        }
    }

    // Setup local video
    LaunchedEffect(localSurfaceView, isVideoEnabled) {
        localSurfaceView?.let { surface ->
            if (isVideoEnabled) {
                agoraManager.setupLocalVideo(surface)
            }
        }
    }

    // Setup remote video
    LaunchedEffect(remoteSurfaceView, remoteUsers) {
        val remoteUid = remoteUsers.firstOrNull()
        if (remoteUid != null && remoteSurfaceView != null) {
            agoraManager.setupRemoteVideo(remoteUid, remoteSurfaceView!!)
        }
    }

    // Handle call end
    LaunchedEffect(callState) {
        if (callState is AgoraManager.CallState.Ended) {
            onCallEnd()
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            agoraManager.leaveCall()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Remote video (full screen)
        if (isVideo && remoteUsers.isNotEmpty()) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        remoteSurfaceView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Avatar placeholder for voice call or when no remote video
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (callState) {
                        is AgoraManager.CallState.Connecting -> "Connecting..."
                        is AgoraManager.CallState.Connected -> formatDuration(callDuration)
                        is AgoraManager.CallState.Reconnecting -> "Reconnecting..."
                        is AgoraManager.CallState.Error -> (callState as AgoraManager.CallState.Error).message
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Local video (small preview)
        if (isVideo && isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            localSurfaceView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Switch camera button
                IconButton(
                    onClick = { agoraManager.switchCamera() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Call info (top)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isVideo && remoteUsers.isNotEmpty()) {
                    Text(
                        text = callerName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (callState is AgoraManager.CallState.Connected)
                            formatDuration(callDuration)
                        else
                            "Connecting...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Controls (bottom)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                CallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = isMuted,
                    onClick = { agoraManager.toggleMute() }
                )

                // Speaker button (voice call only)
                if (!isVideo) {
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = if (isSpeakerOn) "Speaker" else "Earpiece",
                        isActive = isSpeakerOn,
                        onClick = { agoraManager.toggleSpeaker() }
                    )
                }

                // Video toggle (video call only)
                if (isVideo) {
                    CallControlButton(
                        icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = if (isVideoEnabled) "Video On" else "Video Off",
                        isActive = !isVideoEnabled,
                        onClick = { agoraManager.toggleVideo() }
                    )
                }

                // End call button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .clickable {
                            agoraManager.leaveCall()
                            onCallEnd()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Connection status overlay
        if (callState is AgoraManager.CallState.Connecting ||
            callState is AgoraManager.CallState.Reconnecting
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (callState is AgoraManager.CallState.Reconnecting)
                            "Reconnecting..."
                        else
                            "Connecting...",
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Call control button
 */
@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.White else Color.White.copy(alpha = 0.2f)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

/**
 * Format duration in mm:ss
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
