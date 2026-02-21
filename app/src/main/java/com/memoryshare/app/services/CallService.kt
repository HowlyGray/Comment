package com.memoryshare.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.memoryshare.app.MainActivity
import com.memoryshare.app.R
import com.memoryshare.app.services.calls.JitsiMeetManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Call Service
 * Foreground service for handling audio/video calls
 * Keeps calls alive when app is in background
 */
class CallService : Service() {

    companion object {
        private const val TAG = "CallService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "memoryshare_call_service"

        // Actions
        const val ACTION_START_VOICE_CALL = "com.memoryshare.START_VOICE_CALL"
        const val ACTION_START_VIDEO_CALL = "com.memoryshare.START_VIDEO_CALL"
        const val ACTION_END_CALL = "com.memoryshare.END_CALL"
        const val ACTION_MUTE = "com.memoryshare.MUTE"
        const val ACTION_UNMUTE = "com.memoryshare.UNMUTE"
        const val ACTION_SPEAKER_ON = "com.memoryshare.SPEAKER_ON"
        const val ACTION_SPEAKER_OFF = "com.memoryshare.SPEAKER_OFF"
        const val ACTION_TOGGLE_VIDEO = "com.memoryshare.TOGGLE_VIDEO"
        const val ACTION_DECLINE_CALL = "DECLINE_CALL"

        // Extras
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_CALLER_NAME = "caller_name"
        const val EXTRA_CALLER_ID = "caller_id"
        const val EXTRA_CALL_ID = "callId"
        const val EXTRA_IS_VIDEO = "is_video"
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_AVATAR_URL = "avatar_url"

        // Helper method to start call service
        fun startVoiceCall(
            context: Context,
            channelName: String,
            callerName: String,
            callerId: String,
            displayName: String? = null,
            avatarUrl: String? = null
        ) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_START_VOICE_CALL
                putExtra(EXTRA_CHANNEL_NAME, channelName)
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALLER_ID, callerId)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
                putExtra(EXTRA_AVATAR_URL, avatarUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startVideoCall(
            context: Context,
            channelName: String,
            callerName: String,
            callerId: String,
            displayName: String? = null,
            avatarUrl: String? = null
        ) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_START_VIDEO_CALL
                putExtra(EXTRA_CHANNEL_NAME, channelName)
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALLER_ID, callerId)
                putExtra(EXTRA_DISPLAY_NAME, displayName)
                putExtra(EXTRA_AVATAR_URL, avatarUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun endCall(context: Context) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_END_CALL
            }
            context.startService(intent)
        }
    }

    // Binder for activity binding
    private val binder = CallServiceBinder()

    // Jitsi Meet manager
    lateinit var jitsiMeetManager: JitsiMeetManager
        private set

    // Service state
    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Idle)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _currentCall = MutableStateFlow<CallInfo?>(null)
    val currentCall: StateFlow<CallInfo?> = _currentCall.asStateFlow()

    // Coroutine scope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var durationJob: Job? = null

    // Wake lock
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallService created")

        createNotificationChannel()
        jitsiMeetManager = JitsiMeetManager(applicationContext)
        jitsiMeetManager.initialize()

        // Register event receiver for Jitsi events
        jitsiMeetManager.registerEventReceiver(this)

        // Setup call event listener
        jitsiMeetManager.onCallEventListener = object : JitsiMeetManager.CallEventListener {
            override fun onCallConnected() {
                _serviceState.value = ServiceState.InCall
                startDurationCounter()
                updateNotification("In call...")
            }

            override fun onCallEnded() {
                stopSelf()
            }

            override fun onRemoteUserJoined(data: HashMap<String, Any>?) {
                Log.d(TAG, "Remote user joined")
            }

            override fun onRemoteUserLeft(data: HashMap<String, Any>?) {
                Log.d(TAG, "Remote user left")
            }

            override fun onError(code: Int, message: String) {
                Log.e(TAG, "Call error: $code - $message")
                _serviceState.value = ServiceState.Error(message)
            }

            override fun onConnectionLost() {
                updateNotification("Reconnecting...")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_VOICE_CALL -> {
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: return START_NOT_STICKY
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown"
                val callerId = intent.getStringExtra(EXTRA_CALLER_ID) ?: ""
                val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME)
                val avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL)

                startForegroundWithNotification("Voice call with $callerName", false)
                startVoiceCallInternal(channelName, callerName, callerId, displayName, avatarUrl)
            }

            ACTION_START_VIDEO_CALL -> {
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: return START_NOT_STICKY
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown"
                val callerId = intent.getStringExtra(EXTRA_CALLER_ID) ?: ""
                val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME)
                val avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL)

                startForegroundWithNotification("Video call with $callerName", true)
                startVideoCallInternal(channelName, callerName, callerId, displayName, avatarUrl)
            }

            ACTION_END_CALL, ACTION_DECLINE_CALL -> {
                endCurrentCall()
            }

            ACTION_MUTE -> {
                jitsiMeetManager.setMuted(true)
            }

            ACTION_UNMUTE -> {
                jitsiMeetManager.setMuted(false)
            }

            ACTION_TOGGLE_VIDEO -> {
                jitsiMeetManager.toggleVideo()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallService destroyed")

        durationJob?.cancel()
        serviceScope.cancel()
        jitsiMeetManager.unregisterEventReceiver(this)
        jitsiMeetManager.destroy()
        releaseWakeLock()

        _serviceState.value = ServiceState.Idle
        _currentCall.value = null
    }

    /**
     * Start voice call internally
     */
    private fun startVoiceCallInternal(
        channelName: String,
        callerName: String,
        callerId: String,
        displayName: String?,
        avatarUrl: String?
    ) {
        _currentCall.value = CallInfo(
            channelName = channelName,
            callerName = callerName,
            callerId = callerId,
            isVideo = false,
            startTime = System.currentTimeMillis()
        )

        _serviceState.value = ServiceState.Connecting
        acquireWakeLock()

        val result = jitsiMeetManager.joinVoiceCall(channelName, displayName, avatarUrl)
        if (result.isFailure) {
            Log.e(TAG, "Failed to start voice call", result.exceptionOrNull())
            _serviceState.value = ServiceState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            stopSelf()
        }
    }

    /**
     * Start video call internally
     */
    private fun startVideoCallInternal(
        channelName: String,
        callerName: String,
        callerId: String,
        displayName: String?,
        avatarUrl: String?
    ) {
        _currentCall.value = CallInfo(
            channelName = channelName,
            callerName = callerName,
            callerId = callerId,
            isVideo = true,
            startTime = System.currentTimeMillis()
        )

        _serviceState.value = ServiceState.Connecting
        acquireWakeLock()

        val result = jitsiMeetManager.joinVideoCall(channelName, displayName, avatarUrl)
        if (result.isFailure) {
            Log.e(TAG, "Failed to start video call", result.exceptionOrNull())
            _serviceState.value = ServiceState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            stopSelf()
        }
    }

    /**
     * End current call
     */
    fun endCurrentCall() {
        Log.d(TAG, "Ending call")
        durationJob?.cancel()
        jitsiMeetManager.leaveCall()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Start duration counter
     */
    private fun startDurationCounter() {
        durationJob?.cancel()
        durationJob = serviceScope.launch {
            val startTime = _currentCall.value?.startTime ?: System.currentTimeMillis()
            while (isActive) {
                val duration = System.currentTimeMillis() - startTime
                jitsiMeetManager.updateDuration(duration)
                delay(1000)
            }
        }
    }

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ongoing call notifications"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start foreground with notification
     */
    private fun startForegroundWithNotification(text: String, isVideo: Boolean) {
        val notification = buildNotification(text, isVideo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (isVideo) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Update notification
     */
    private fun updateNotification(text: String) {
        val notification = buildNotification(text, _currentCall.value?.isVideo == true)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Build notification
     */
    private fun buildNotification(text: String, isVideo: Boolean): Notification {
        // Intent to open app
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // End call action
        val endIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endPendingIntent = PendingIntent.getService(
            this, 1, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mute action
        val isMuted = jitsiMeetManager.isMuted.value
        val muteIntent = Intent(this, CallService::class.java).apply {
            action = if (isMuted) ACTION_UNMUTE else ACTION_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this, 2, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isVideo) "Video Call" else "Voice Call")
            .setContentText(text)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(
                R.drawable.ic_notification,
                if (isMuted) "Unmute" else "Mute",
                mutePendingIntent
            )
            .addAction(
                R.drawable.ic_notification,
                "End",
                endPendingIntent
            )

        // Show duration if in call
        val duration = jitsiMeetManager.callDuration.value
        if (duration > 0) {
            val minutes = duration / 60000
            val seconds = (duration % 60000) / 1000
            builder.setContentText("$text - ${String.format("%02d:%02d", minutes, seconds)}")
        }

        return builder.build()
    }

    /**
     * Acquire wake lock
     */
    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MemoryShare::CallWakeLock"
            )
            wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max
        }
    }

    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    /**
     * Service binder
     */
    inner class CallServiceBinder : Binder() {
        fun getService(): CallService = this@CallService
    }

    /**
     * Service state
     */
    sealed class ServiceState {
        data object Idle : ServiceState()
        data object Connecting : ServiceState()
        data object InCall : ServiceState()
        data class Error(val message: String) : ServiceState()
    }

    /**
     * Call info
     */
    data class CallInfo(
        val channelName: String,
        val callerName: String,
        val callerId: String,
        val isVideo: Boolean,
        val startTime: Long
    )
}
