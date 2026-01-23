package com.memoryshare.app.services.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX Manager
 * Handles camera preview, photo capture, and video recording
 */
class CameraManager(private val context: Context) {

    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    // Camera state
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private val _currentLensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val currentLensFacing: StateFlow<Int> = _currentLensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    // Camera components
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var recording: Recording? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Initialize camera and bind to lifecycle
     */
    suspend fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        enableVideo: Boolean = true
    ) = suspendCancellableCoroutine { continuation ->
        _cameraState.value = CameraState.Initializing

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Build preview
                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                // Build image capture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setFlashMode(_flashMode.value)
                    .build()

                // Build video capture if enabled
                if (enableVideo) {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.HD,
                                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                            )
                        )
                        .build()

                    videoCapture = VideoCapture.withOutput(recorder)
                }

                // Bind use cases
                bindCameraUseCases(lifecycleOwner, enableVideo)

                _cameraState.value = CameraState.Ready
                continuation.resume(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                _cameraState.value = CameraState.Error(e.message ?: "Unknown error")
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))

        continuation.invokeOnCancellation {
            shutdown()
        }
    }

    /**
     * Bind camera use cases
     */
    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, enableVideo: Boolean) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(_currentLensFacing.value)
            .build()

        try {
            cameraProvider?.unbindAll()

            val useCases = mutableListOf<UseCase>(preview!!, imageCapture!!)
            if (enableVideo && videoCapture != null) {
                useCases.add(videoCapture!!)
            }

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
            _cameraState.value = CameraState.Error(e.message ?: "Binding failed")
        }
    }

    /**
     * Take photo
     */
    suspend fun takePhoto(): Result<Uri> = suspendCancellableCoroutine { continuation ->
        val imageCapture = imageCapture ?: run {
            continuation.resume(Result.failure(Exception("Camera not initialized")))
            return@suspendCancellableCoroutine
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MemoryShare")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.EMPTY
                    Log.d(TAG, "Photo saved: $savedUri")
                    continuation.resume(Result.success(savedUri))
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    continuation.resume(Result.failure(exception))
                }
            }
        )
    }

    /**
     * Take photo to file
     */
    suspend fun takePhotoToFile(outputFile: File): Result<File> =
        suspendCancellableCoroutine { continuation ->
            val imageCapture = imageCapture ?: run {
                continuation.resume(Result.failure(Exception("Camera not initialized")))
                return@suspendCancellableCoroutine
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d(TAG, "Photo saved to file: ${outputFile.absolutePath}")
                        continuation.resume(Result.success(outputFile))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed", exception)
                        continuation.resume(Result.failure(exception))
                    }
                }
            )
        }

    /**
     * Start video recording
     */
    @androidx.annotation.OptIn(ExperimentalPersistentRecording::class)
    fun startRecording(onFinished: (Result<Uri>) -> Unit) {
        val videoCapture = videoCapture ?: run {
            onFinished(Result.failure(Exception("Video capture not initialized")))
            return
        }

        if (_isRecording.value) {
            Log.w(TAG, "Already recording")
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MemoryShare")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
                // Enable audio if permission granted
                try {
                    withAudioEnabled()
                } catch (e: SecurityException) {
                    Log.w(TAG, "Audio permission not granted")
                }
            }
            .start(cameraExecutor) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        _isRecording.value = true
                        _recordingDuration.value = 0L
                        Log.d(TAG, "Recording started")
                    }

                    is VideoRecordEvent.Status -> {
                        _recordingDuration.value = recordEvent.recordingStats.recordedDurationNanos / 1_000_000
                    }

                    is VideoRecordEvent.Finalize -> {
                        _isRecording.value = false
                        if (!recordEvent.hasError()) {
                            val uri = recordEvent.outputResults.outputUri
                            Log.d(TAG, "Recording saved: $uri")
                            onFinished(Result.success(uri))
                        } else {
                            Log.e(TAG, "Recording error: ${recordEvent.error}")
                            onFinished(Result.failure(Exception("Recording failed: ${recordEvent.error}")))
                        }
                    }
                }
            }
    }

    /**
     * Stop video recording
     */
    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    /**
     * Pause video recording
     */
    fun pauseRecording() {
        recording?.pause()
    }

    /**
     * Resume video recording
     */
    fun resumeRecording() {
        recording?.resume()
    }

    /**
     * Switch camera (front/back)
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner, enableVideo: Boolean = true) {
        _currentLensFacing.value = if (_currentLensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        bindCameraUseCases(lifecycleOwner, enableVideo)
    }

    /**
     * Toggle flash mode
     */
    fun toggleFlash() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = _flashMode.value
    }

    /**
     * Set flash mode
     */
    fun setFlashMode(mode: Int) {
        _flashMode.value = mode
        imageCapture?.flashMode = mode
    }

    /**
     * Enable/disable torch
     */
    fun enableTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * Set zoom ratio
     */
    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    /**
     * Set linear zoom (0.0 to 1.0)
     */
    fun setLinearZoom(zoom: Float) {
        camera?.cameraControl?.setLinearZoom(zoom.coerceIn(0f, 1f))
    }

    /**
     * Focus on point
     */
    fun focusOnPoint(x: Float, y: Float, previewView: PreviewView) {
        val meteringPointFactory = previewView.meteringPointFactory
        val point = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    /**
     * Check if front camera available
     */
    fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * Check if back camera available
     */
    fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /**
     * Get current zoom state
     */
    fun getZoomState() = camera?.cameraInfo?.zoomState

    /**
     * Shutdown camera
     */
    fun shutdown() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        _cameraState.value = CameraState.Idle
    }

    /**
     * Camera state
     */
    sealed class CameraState {
        data object Idle : CameraState()
        data object Initializing : CameraState()
        data object Ready : CameraState()
        data class Error(val message: String) : CameraState()
    }
}
