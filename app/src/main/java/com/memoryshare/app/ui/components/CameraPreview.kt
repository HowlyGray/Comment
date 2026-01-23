package com.memoryshare.app.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.memoryshare.app.MemoryShareApplication
import com.memoryshare.app.services.media.CameraManager
import kotlinx.coroutines.launch

/**
 * Camera Preview Composable
 * Uses CameraX for camera preview and capture
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    enableVideo: Boolean = true,
    onPhotoTaken: (Uri) -> Unit = {},
    onVideoRecorded: (Uri) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val app = context.applicationContext as MemoryShareApplication
    val cameraManager = remember { app.cameraManager }

    val cameraState by cameraManager.cameraState.collectAsState()
    val isRecording by cameraManager.isRecording.collectAsState()
    val recordingDuration by cameraManager.recordingDuration.collectAsState()
    val currentLensFacing by cameraManager.currentLensFacing.collectAsState()
    val flashMode by cameraManager.flashMode.collectAsState()

    var isVideoMode by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Initialize camera
    LaunchedEffect(Unit) {
        previewView?.let { pv ->
            try {
                cameraManager.initialize(lifecycleOwner, pv, enableVideo)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Flash toggle
            IconButton(
                onClick = { cameraManager.toggleFlash() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        androidx.camera.core.ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        androidx.camera.core.ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }

        // Recording indicator
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(recordingDuration),
                    color = Color.White
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode selector (Photo/Video)
            if (enableVideo && !isRecording) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { isVideoMode = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (!isVideoMode) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("Photo")
                    }
                    TextButton(
                        onClick = { isVideoMode = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isVideoMode) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("Video")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Capture controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button (placeholder)
                IconButton(
                    onClick = { /* Open gallery */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }

                // Capture/Record button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (isVideoMode) {
                                    if (isRecording) {
                                        cameraManager.stopRecording()
                                    } else {
                                        cameraManager.startRecording { result ->
                                            result.onSuccess { uri ->
                                                onVideoRecorded(uri)
                                            }
                                        }
                                    }
                                } else {
                                    val result = cameraManager.takePhoto()
                                    result.onSuccess { uri ->
                                        onPhotoTaken(uri)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(60.dp)
                    ) {
                        if (isVideoMode) {
                            Box(
                                modifier = Modifier
                                    .size(if (isRecording) 24.dp else 40.dp)
                                    .background(
                                        Color.Red,
                                        if (isRecording) RoundedCornerShape(4.dp) else CircleShape
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }

                // Switch camera button
                IconButton(
                    onClick = { cameraManager.switchCamera(lifecycleOwner, enableVideo) },
                    modifier = Modifier.size(48.dp),
                    enabled = !isRecording
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Loading/Error overlay
        when (cameraState) {
            is CameraManager.CameraState.Initializing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is CameraManager.CameraState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (cameraState as CameraManager.CameraState.Error).message,
                            color = Color.White
                        )
                    }
                }
            }
            else -> { /* Ready or Idle */ }
        }
    }
}

/**
 * Format duration in mm:ss
 */
private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / 1000) / 60
    return String.format("%02d:%02d", minutes, seconds)
}
