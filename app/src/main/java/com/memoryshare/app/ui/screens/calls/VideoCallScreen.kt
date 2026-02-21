package com.memoryshare.app.ui.screens.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoryshare.app.MemoryShareApplication
import com.memoryshare.app.services.calls.JitsiMeetManager

/**
 * Video Call Screen
 * Launches a Jitsi Meet call and shows a pre-call / post-call UI.
 * The actual call rendering is handled by JitsiMeetActivity (Jitsi SDK).
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
    val jitsiMeetManager = remember { app.jitsiMeetManager }

    val callState by jitsiMeetManager.callState.collectAsState()
    val isMuted by jitsiMeetManager.isMuted.collectAsState()
    val isVideoEnabled by jitsiMeetManager.isVideoEnabled.collectAsState()
    val callDuration by jitsiMeetManager.callDuration.collectAsState()

    // Launch the Jitsi call on first composition
    LaunchedEffect(Unit) {
        if (isVideo) {
            jitsiMeetManager.joinVideoCall(channelName, callerName)
        } else {
            jitsiMeetManager.joinVoiceCall(channelName, callerName)
        }
    }

    // Handle call end from Jitsi
    LaunchedEffect(callState) {
        if (callState is JitsiMeetManager.CallState.Ended) {
            onCallEnd()
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            jitsiMeetManager.leaveCall()
        }
    }

    // This screen serves as a fallback UI while the Jitsi Activity is active.
    // It is visible when the user navigates back from the Jitsi PiP or when
    // the call hasn't started yet / has ended.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Center content: avatar + caller info + status
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
                    is JitsiMeetManager.CallState.Connecting -> "Connecting..."
                    is JitsiMeetManager.CallState.Connected -> formatDuration(callDuration)
                    is JitsiMeetManager.CallState.Reconnecting -> "Reconnecting..."
                    is JitsiMeetManager.CallState.Error ->
                        (callState as JitsiMeetManager.CallState.Error).message
                    is JitsiMeetManager.CallState.Ended -> "Call ended"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute button
            CallControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                isActive = isMuted,
                onClick = { jitsiMeetManager.toggleMute() }
            )

            // Video toggle (video call only)
            if (isVideo) {
                CallControlButton(
                    icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (isVideoEnabled) "Video On" else "Video Off",
                    isActive = !isVideoEnabled,
                    onClick = { jitsiMeetManager.toggleVideo() }
                )
            }

            // End call button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .clickable {
                        jitsiMeetManager.leaveCall()
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

        // Connection status overlay
        if (callState is JitsiMeetManager.CallState.Connecting ||
            callState is JitsiMeetManager.CallState.Reconnecting
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
                        text = if (callState is JitsiMeetManager.CallState.Reconnecting)
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
