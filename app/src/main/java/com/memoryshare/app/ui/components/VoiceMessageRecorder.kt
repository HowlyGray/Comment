package com.memoryshare.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.memoryshare.app.MemoryShareApplication
import com.memoryshare.app.services.media.AudioRecorderManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Voice Message Recorder Component
 * Press and hold to record, release to send
 */
@Composable
fun VoiceMessageRecorder(
    modifier: Modifier = Modifier,
    onRecordingComplete: (File, Long) -> Unit,
    onRecordingCancelled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val app = context.applicationContext as MemoryShareApplication
    val audioRecorder = remember { app.audioRecorderManager }

    val recordingState by audioRecorder.recordingState.collectAsState()
    val recordingDuration by audioRecorder.recordingDuration.collectAsState()
    val amplitudes by audioRecorder.amplitudes.collectAsState()
    val currentAmplitude by audioRecorder.currentAmplitude.collectAsState()

    var isPressed by remember { mutableStateOf(false) }
    var slideToCancel by remember { mutableStateOf(0f) }

    // Pulse animation for recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isRecording = recordingState is AudioRecorderManager.RecordingState.Recording

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Slide to cancel indicator
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Waveform visualization
                    WaveformVisualizer(
                        amplitudes = amplitudes,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Duration
                    Text(
                        text = formatDuration(recordingDuration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Slide to cancel
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Slide to cancel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Record button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(if (isRecording) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                // Start recording
                                scope.launch {
                                    audioRecorder.startRecording()
                                }

                                // Wait for release
                                val released = tryAwaitRelease()

                                isPressed = false

                                if (released) {
                                    // Stop recording and get result
                                    val result = audioRecorder.stopRecording()
                                    result.onSuccess { recordingResult ->
                                        if (recordingResult.duration > 1000) { // Min 1 second
                                            onRecordingComplete(
                                                recordingResult.file,
                                                recordingResult.duration
                                            )
                                        } else {
                                            recordingResult.file.delete()
                                            onRecordingCancelled()
                                        }
                                    }
                                } else {
                                    // Cancelled
                                    audioRecorder.cancelRecording()
                                    onRecordingCancelled()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Record voice message",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Waveform Visualizer
 * Shows audio amplitude as a waveform
 */
@Composable
fun WaveformVisualizer(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val maxAmplitude = 32767f // Max amplitude value

    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val barWidth = width / amplitudes.size.coerceAtLeast(1)
        val centerY = height / 2

        amplitudes.forEachIndexed { index, amplitude ->
            val normalizedAmplitude = (amplitude / maxAmplitude).coerceIn(0.1f, 1f)
            val barHeight = height * normalizedAmplitude

            val x = index * barWidth + barWidth / 2

            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth * 0.6f
            )
        }
    }
}

/**
 * Compact voice recorder button
 * For use in chat input
 */
@Composable
fun VoiceRecorderButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    recordingDuration: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isRecording) {
            // Cancel button
            IconButton(
                onClick = onCancelRecording,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Duration
            Text(
                text = formatDuration(recordingDuration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            // Recording indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(pulse)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Stop/Send button
            IconButton(
                onClick = onStopRecording,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        } else {
            // Mic button
            IconButton(
                onClick = onStartRecording,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record voice message",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Audio player for voice messages
 */
@Composable
fun VoiceMessagePlayer(
    audioUrl: String,
    duration: Long,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    onPlayPause: () -> Unit = {},
    onSeek: (Float) -> Unit = {}
) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(24.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Progress bar
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Duration
            Text(
                text = formatDuration((duration * progress).toLong()) + " / " + formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
