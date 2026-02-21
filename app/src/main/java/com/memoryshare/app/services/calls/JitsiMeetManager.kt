package com.memoryshare.app.services.calls

import android.content.Context
import android.content.Intent
import android.util.Log
import com.memoryshare.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jitsi.meet.sdk.*
import java.net.URL

/**
 * Jitsi Meet Manager
 * Handles voice and video calls using Jitsi Meet SDK.
 * Replaces the former Agora-based AgoraManager.
 */
class JitsiMeetManager(private val context: Context) {

    companion object {
        private const val TAG = "JitsiMeetManager"

        val SERVER_URL: String = BuildConfig.JITSI_SERVER_URL
    }

    // Call state
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Audio state
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    // Video state
    private val _isVideoEnabled = MutableStateFlow(false)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    // Call duration
    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    // Current room name
    private var currentRoom: String? = null

    // Callback for call events
    var onCallEventListener: CallEventListener? = null

    // Jitsi broadcast receiver for events
    private var broadcastReceiver: BroadcastReceiver? = null

    /**
     * Initialize the Jitsi Meet SDK with default configuration.
     * Should be called once at Application startup.
     */
    fun initialize(): Result<Unit> {
        return try {
            val serverUrl = try {
                URL(SERVER_URL)
            } catch (_: Exception) {
                URL("https://meet.jit.si")
            }

            val defaultOptions = JitsiMeetConferenceOptions.Builder()
                .setServerURL(serverUrl)
                .setFeatureFlag("welcomepage.enabled", false)
                .setFeatureFlag("prejoinpage.enabled", false)
                .setFeatureFlag("recording.enabled", false)
                .setFeatureFlag("live-streaming.enabled", false)
                .setFeatureFlag("invite.enabled", false)
                .setFeatureFlag("chat.enabled", false)
                .setFeatureFlag("raise-hand.enabled", false)
                .setFeatureFlag("tile-view.enabled", false)
                .build()

            JitsiMeet.setDefaultConferenceOptions(defaultOptions)

            Log.d(TAG, "Jitsi Meet SDK initialized with server: $SERVER_URL")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Jitsi Meet SDK", e)
            _callState.value = CallState.Error("Initialization failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Register the broadcast receiver for Jitsi conference events.
     * Must be called when a host Activity is available (e.g. in onCreate).
     */
    fun registerEventReceiver(context: Context) {
        broadcastReceiver = object : BroadcastReceiver(context) {
            override fun onConferenceJoined(data: HashMap<String, Any>?) {
                Log.d(TAG, "Conference joined: $data")
                _callState.value = CallState.Connected
                onCallEventListener?.onCallConnected()
            }

            override fun onConferenceTerminated(data: HashMap<String, Any>?) {
                Log.d(TAG, "Conference terminated: $data")
                _callState.value = CallState.Ended
                _callDuration.value = 0L
                currentRoom = null
                onCallEventListener?.onCallEnded()
            }

            override fun onConferenceWillJoin(data: HashMap<String, Any>?) {
                Log.d(TAG, "Conference will join: $data")
                _callState.value = CallState.Connecting
            }

            override fun onParticipantJoined(data: HashMap<String, Any>?) {
                Log.d(TAG, "Participant joined: $data")
                onCallEventListener?.onRemoteUserJoined(data)
            }

            override fun onParticipantLeft(data: HashMap<String, Any>?) {
                Log.d(TAG, "Participant left: $data")
                onCallEventListener?.onRemoteUserLeft(data)
            }

            override fun onAudioMutedChanged(data: HashMap<String, Any>?) {
                val muted = data?.get("muted") as? Boolean ?: false
                _isMuted.value = muted
            }

            override fun onVideoMutedChanged(data: HashMap<String, Any>?) {
                val muted = data?.get("muted") as? Boolean ?: true
                _isVideoEnabled.value = !muted
            }
        }
    }

    /**
     * Unregister the broadcast receiver.
     */
    fun unregisterEventReceiver(context: Context) {
        broadcastReceiver?.let {
            org.jitsi.meet.sdk.BroadcastReceiver.unregister(context, it)
        }
        broadcastReceiver = null
    }

    /**
     * Join a voice call (audio only).
     */
    fun joinVoiceCall(
        roomName: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ): Result<Unit> {
        return joinCall(roomName, isVideo = false, displayName = displayName, avatarUrl = avatarUrl)
    }

    /**
     * Join a video call (audio + video).
     */
    fun joinVideoCall(
        roomName: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ): Result<Unit> {
        return joinCall(roomName, isVideo = true, displayName = displayName, avatarUrl = avatarUrl)
    }

    /**
     * Internal join call implementation.
     */
    private fun joinCall(
        roomName: String,
        isVideo: Boolean,
        displayName: String?,
        avatarUrl: String?
    ): Result<Unit> {
        return try {
            _callState.value = CallState.Connecting
            currentRoom = roomName
            _isVideoEnabled.value = isVideo

            val userInfoBuilder = JitsiMeetUserInfo()
            displayName?.let { userInfoBuilder.displayName = it }
            avatarUrl?.let {
                try { userInfoBuilder.avatar = URL(it) } catch (_: Exception) {}
            }

            val options = JitsiMeetConferenceOptions.Builder()
                .setRoom(roomName)
                .setAudioMuted(false)
                .setVideoMuted(!isVideo)
                .setUserInfo(userInfoBuilder)
                .setFeatureFlag("pip.enabled", true)
                .setFeatureFlag("add-people.enabled", false)
                .setFeatureFlag("calendar.enabled", false)
                .setFeatureFlag("close-captions.enabled", false)
                .setFeatureFlag("help.enabled", false)
                .setFeatureFlag("kick-out.enabled", false)
                .setFeatureFlag("meeting-name.enabled", false)
                .setFeatureFlag("meeting-password.enabled", false)
                .setFeatureFlag("reactions.enabled", false)
                .setFeatureFlag("server-url-change.enabled", false)
                .setFeatureFlag("video-share.enabled", false)
                .build()

            // Launch the Jitsi Meet Activity
            JitsiMeetActivity.launch(context, options)

            Log.d(TAG, "Joining ${if (isVideo) "video" else "voice"} call: $roomName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join call", e)
            _callState.value = CallState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Leave current call.
     */
    fun leaveCall(): Result<Unit> {
        return try {
            val hangUpIntent = BroadcastIntentHelper.buildHangUpIntent()
            context.sendBroadcast(hangUpIntent)

            currentRoom = null
            _callState.value = CallState.Ended
            _callDuration.value = 0L
            _isMuted.value = false
            _isVideoEnabled.value = false

            Log.d(TAG, "Left call")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave call", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle microphone mute.
     */
    fun toggleMute(): Boolean {
        val newState = !_isMuted.value
        val intent = BroadcastIntentHelper.buildSetAudioMutedIntent(newState)
        context.sendBroadcast(intent)
        _isMuted.value = newState
        return newState
    }

    /**
     * Set mute state.
     */
    fun setMuted(muted: Boolean) {
        val intent = BroadcastIntentHelper.buildSetAudioMutedIntent(muted)
        context.sendBroadcast(intent)
        _isMuted.value = muted
    }

    /**
     * Toggle video.
     */
    fun toggleVideo(): Boolean {
        val newState = !_isVideoEnabled.value
        val intent = BroadcastIntentHelper.buildSetVideoMutedIntent(!newState)
        context.sendBroadcast(intent)
        _isVideoEnabled.value = newState
        return newState
    }

    /**
     * Set video enabled/disabled.
     */
    fun setVideoEnabled(enabled: Boolean) {
        val intent = BroadcastIntentHelper.buildSetVideoMutedIntent(!enabled)
        context.sendBroadcast(intent)
        _isVideoEnabled.value = enabled
    }

    /**
     * Update call duration (called by the service timer).
     */
    fun updateDuration(duration: Long) {
        _callDuration.value = duration
    }

    /**
     * Destroy the manager and release resources.
     */
    fun destroy() {
        try {
            if (currentRoom != null) {
                leaveCall()
            }
            _callState.value = CallState.Idle
            Log.d(TAG, "JitsiMeetManager destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying JitsiMeetManager", e)
        }
    }

    /**
     * Call state sealed class.
     */
    sealed class CallState {
        data object Idle : CallState()
        data object Connecting : CallState()
        data object Connected : CallState()
        data object Reconnecting : CallState()
        data object Ended : CallState()
        data class Error(val message: String) : CallState()
    }

    /**
     * Call event listener interface.
     */
    interface CallEventListener {
        fun onCallConnected() {}
        fun onCallEnded() {}
        fun onRemoteUserJoined(data: HashMap<String, Any>?) {}
        fun onRemoteUserLeft(data: HashMap<String, Any>?) {}
        fun onError(code: Int, message: String) {}
        fun onConnectionLost() {}
    }

    /**
     * Jitsi Meet BroadcastReceiver for handling conference events.
     */
    abstract class BroadcastReceiver(context: Context) :
        org.jitsi.meet.sdk.BroadcastReceiver(context) {

        open fun onParticipantJoined(data: HashMap<String, Any>?) {}
        open fun onParticipantLeft(data: HashMap<String, Any>?) {}
    }
}
