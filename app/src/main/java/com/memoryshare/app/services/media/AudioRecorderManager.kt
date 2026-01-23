package com.memoryshare.app.services.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Audio Recorder Manager
 * Handles voice message recording with amplitude visualization
 */
class AudioRecorderManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderManager"
        private const val SAMPLE_RATE = 44100
        private const val BIT_RATE = 128000
        private const val MAX_AMPLITUDE_SAMPLES = 50
    }

    // Recording state
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes: StateFlow<List<Int>> = _amplitudes.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0L
    private var isRecording = false
    private var isPaused = false

    /**
     * Start recording
     */
    suspend fun startRecording(outputPath: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (isRecording) {
                return@withContext Result.failure(Exception("Already recording"))
            }

            // Create output file
            outputFile = if (outputPath != null) {
                File(outputPath)
            } else {
                createTempAudioFile()
            }

            // Initialize MediaRecorder
            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioEncodingBitRate(BIT_RATE)
                setOutputFile(outputFile!!.absolutePath)

                try {
                    prepare()
                    start()
                } catch (e: IOException) {
                    Log.e(TAG, "MediaRecorder prepare failed", e)
                    release()
                    throw e
                }
            }

            isRecording = true
            isPaused = false
            startTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording
            _amplitudes.value = emptyList()

            // Start amplitude monitoring
            startAmplitudeMonitoring()

            Result.success(outputFile!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording(): Result<RecordingResult> {
        return try {
            if (!isRecording) {
                return Result.failure(Exception("Not recording"))
            }

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            isRecording = false
            isPaused = false

            val duration = System.currentTimeMillis() - startTime
            _recordingState.value = RecordingState.Completed

            val file = outputFile ?: return Result.failure(Exception("No output file"))

            Result.success(
                RecordingResult(
                    file = file,
                    duration = duration,
                    amplitudes = _amplitudes.value
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            cleanup()
            Result.failure(e)
        }
    }

    /**
     * Pause recording (API 24+)
     */
    fun pauseRecording(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && !isPaused) {
                mediaRecorder?.pause()
                isPaused = true
                _recordingState.value = RecordingState.Paused
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
            false
        }
    }

    /**
     * Resume recording (API 24+)
     */
    fun resumeRecording(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && isPaused) {
                mediaRecorder?.resume()
                isPaused = false
                _recordingState.value = RecordingState.Recording
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
            false
        }
    }

    /**
     * Cancel recording and delete file
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }

        cleanup()
        outputFile?.delete()
        outputFile = null
        _recordingState.value = RecordingState.Idle
    }

    /**
     * Get current recording duration
     */
    fun getCurrentDuration(): Long {
        return if (isRecording && !isPaused) {
            System.currentTimeMillis() - startTime
        } else {
            _recordingDuration.value
        }
    }

    /**
     * Start monitoring amplitude for visualization
     */
    private suspend fun startAmplitudeMonitoring() {
        withContext(Dispatchers.Default) {
            while (coroutineContext.isActive && isRecording) {
                if (!isPaused) {
                    try {
                        val amplitude = mediaRecorder?.maxAmplitude ?: 0
                        _currentAmplitude.value = amplitude

                        // Update amplitude list for waveform
                        val currentList = _amplitudes.value.toMutableList()
                        currentList.add(amplitude)
                        if (currentList.size > MAX_AMPLITUDE_SAMPLES) {
                            currentList.removeAt(0)
                        }
                        _amplitudes.value = currentList

                        // Update duration
                        _recordingDuration.value = System.currentTimeMillis() - startTime
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting amplitude", e)
                    }
                }
                delay(100) // Update every 100ms
            }
        }
    }

    /**
     * Create MediaRecorder instance
     */
    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    /**
     * Create temporary audio file
     */
    private fun createTempAudioFile(): File {
        val audioDir = File(context.cacheDir, "audio_recordings")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return File.createTempFile(
            "voice_message_${System.currentTimeMillis()}",
            ".m4a",
            audioDir
        )
    }

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
        isPaused = false
        _recordingDuration.value = 0L
        _currentAmplitude.value = 0
    }

    /**
     * Release all resources
     */
    fun release() {
        cleanup()
        _recordingState.value = RecordingState.Idle
    }

    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * Check if recording is paused
     */
    fun isCurrentlyPaused(): Boolean = isPaused

    /**
     * Recording state
     */
    sealed class RecordingState {
        data object Idle : RecordingState()
        data object Recording : RecordingState()
        data object Paused : RecordingState()
        data object Completed : RecordingState()
        data class Error(val message: String) : RecordingState()
    }

    /**
     * Recording result
     */
    data class RecordingResult(
        val file: File,
        val duration: Long,
        val amplitudes: List<Int>
    )
}
