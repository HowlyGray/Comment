package com.memoryshare.app.services.calls

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Agora RTC Manager
 * Handles voice and video calls using Agora SDK
 */
class AgoraManager(private val context: Context) {

    companion object {
        private const val TAG = "AgoraManager"

        // Replace with your Agora App ID from https://console.agora.io/
        // IMPORTANT: In production, this should be retrieved from a secure backend
        const val APP_ID = "YOUR_AGORA_APP_ID"
    }

    // RTC Engine
    private var rtcEngine: RtcEngine? = null

    // Call state
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Audio state
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    // Video state
    private val _isVideoEnabled = MutableStateFlow(false)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    // Remote users
    private val _remoteUsers = MutableStateFlow<Set<Int>>(emptySet())
    val remoteUsers: StateFlow<Set<Int>> = _remoteUsers.asStateFlow()

    // Call duration
    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    // Current channel
    private var currentChannel: String? = null
    private var currentToken: String? = null
    private var localUid: Int = 0

    // Callback for call events
    var onCallEventListener: CallEventListener? = null

    /**
     * Initialize Agora RTC Engine
     */
    fun initialize(): Result<Unit> {
        return try {
            if (rtcEngine != null) {
                Log.w(TAG, "RTC Engine already initialized")
                return Result.success(Unit)
            }

            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = APP_ID
                mEventHandler = rtcEventHandler
            }

            rtcEngine = RtcEngine.create(config)

            // Configure audio
            rtcEngine?.apply {
                setAudioProfile(
                    Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO,
                    Constants.AUDIO_SCENARIO_GAME_STREAMING
                )
                enableAudioVolumeIndication(200, 3, true)
            }

            Log.d(TAG, "Agora RTC Engine initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora RTC Engine", e)
            _callState.value = CallState.Error("Initialization failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Join a voice call
     */
    fun joinVoiceCall(
        channelName: String,
        token: String? = null,
        uid: Int = 0
    ): Result<Unit> {
        return try {
            ensureInitialized()

            currentChannel = channelName
            currentToken = token
            localUid = uid

            _callState.value = CallState.Connecting

            // Configure for voice call
            rtcEngine?.apply {
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                disableVideo()
            }

            // Join channel
            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                autoSubscribeAudio = true
                autoSubscribeVideo = false
            }

            val result = rtcEngine?.joinChannel(token, channelName, uid, options)
            if (result != 0) {
                throw Exception("Join channel failed with code: $result")
            }

            _isVideoEnabled.value = false
            Log.d(TAG, "Joining voice call: $channelName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join voice call", e)
            _callState.value = CallState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Join a video call
     */
    fun joinVideoCall(
        channelName: String,
        token: String? = null,
        uid: Int = 0
    ): Result<Unit> {
        return try {
            ensureInitialized()

            currentChannel = channelName
            currentToken = token
            localUid = uid

            _callState.value = CallState.Connecting

            // Configure for video call
            rtcEngine?.apply {
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                enableVideo()

                // Set video configuration
                setVideoEncoderConfiguration(
                    VideoEncoderConfiguration(
                        VideoEncoderConfiguration.VD_640x360,
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                        VideoEncoderConfiguration.STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                    )
                )
            }

            // Join channel
            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                autoSubscribeAudio = true
                autoSubscribeVideo = true
            }

            val result = rtcEngine?.joinChannel(token, channelName, uid, options)
            if (result != 0) {
                throw Exception("Join channel failed with code: $result")
            }

            _isVideoEnabled.value = true
            Log.d(TAG, "Joining video call: $channelName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join video call", e)
            _callState.value = CallState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Leave current call
     */
    fun leaveCall(): Result<Unit> {
        return try {
            rtcEngine?.leaveChannel()
            currentChannel = null
            currentToken = null
            _remoteUsers.value = emptySet()
            _callState.value = CallState.Ended
            _callDuration.value = 0L
            Log.d(TAG, "Left call")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave call", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle microphone mute
     */
    fun toggleMute(): Boolean {
        val newState = !_isMuted.value
        rtcEngine?.muteLocalAudioStream(newState)
        _isMuted.value = newState
        Log.d(TAG, "Mute toggled: $newState")
        return newState
    }

    /**
     * Set mute state
     */
    fun setMuted(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
        _isMuted.value = muted
    }

    /**
     * Toggle speaker
     */
    fun toggleSpeaker(): Boolean {
        val newState = !_isSpeakerOn.value
        rtcEngine?.setEnableSpeakerphone(newState)
        _isSpeakerOn.value = newState
        Log.d(TAG, "Speaker toggled: $newState")
        return newState
    }

    /**
     * Set speaker state
     */
    fun setSpeakerEnabled(enabled: Boolean) {
        rtcEngine?.setEnableSpeakerphone(enabled)
        _isSpeakerOn.value = enabled
    }

    /**
     * Toggle video
     */
    fun toggleVideo(): Boolean {
        val newState = !_isVideoEnabled.value
        if (newState) {
            rtcEngine?.enableVideo()
            rtcEngine?.startPreview()
        } else {
            rtcEngine?.disableVideo()
            rtcEngine?.stopPreview()
        }
        _isVideoEnabled.value = newState
        Log.d(TAG, "Video toggled: $newState")
        return newState
    }

    /**
     * Enable/disable video
     */
    fun setVideoEnabled(enabled: Boolean) {
        if (enabled) {
            rtcEngine?.enableVideo()
            rtcEngine?.startPreview()
        } else {
            rtcEngine?.disableVideo()
            rtcEngine?.stopPreview()
        }
        _isVideoEnabled.value = enabled
    }

    /**
     * Switch camera
     */
    fun switchCamera(): Boolean {
        val result = rtcEngine?.switchCamera()
        if (result == 0) {
            _isFrontCamera.value = !_isFrontCamera.value
            Log.d(TAG, "Camera switched to: ${if (_isFrontCamera.value) "front" else "back"}")
            return true
        }
        return false
    }

    /**
     * Setup local video view
     */
    fun setupLocalVideo(surfaceView: SurfaceView) {
        rtcEngine?.setupLocalVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, localUid)
        )
        rtcEngine?.startPreview()
    }

    /**
     * Setup local video view with TextureView
     */
    fun setupLocalVideo(textureView: TextureView) {
        rtcEngine?.setupLocalVideo(
            VideoCanvas(textureView, VideoCanvas.RENDER_MODE_HIDDEN, localUid)
        )
        rtcEngine?.startPreview()
    }

    /**
     * Setup remote video view
     */
    fun setupRemoteVideo(uid: Int, surfaceView: SurfaceView) {
        rtcEngine?.setupRemoteVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }

    /**
     * Setup remote video view with TextureView
     */
    fun setupRemoteVideo(uid: Int, textureView: TextureView) {
        rtcEngine?.setupRemoteVideo(
            VideoCanvas(textureView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }

    /**
     * Mute remote audio
     */
    fun muteRemoteAudio(uid: Int, muted: Boolean) {
        rtcEngine?.muteRemoteAudioStream(uid, muted)
    }

    /**
     * Mute remote video
     */
    fun muteRemoteVideo(uid: Int, muted: Boolean) {
        rtcEngine?.muteRemoteVideoStream(uid, muted)
    }

    /**
     * Get call statistics
     */
    fun getCallStats(): RtcStats? {
        // Stats are provided via callback
        return null
    }

    /**
     * Update call duration
     */
    fun updateDuration(duration: Long) {
        _callDuration.value = duration
    }

    /**
     * Ensure engine is initialized
     */
    private fun ensureInitialized() {
        if (rtcEngine == null) {
            initialize()
        }
    }

    /**
     * Destroy engine and release resources
     */
    fun destroy() {
        try {
            rtcEngine?.stopPreview()
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
            _callState.value = CallState.Idle
            Log.d(TAG, "Agora RTC Engine destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying RTC Engine", e)
        }
    }

    /**
     * RTC Event Handler
     */
    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined channel: $channel, uid: $uid")
            localUid = uid
            _callState.value = CallState.Connected
            onCallEventListener?.onCallConnected()
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "Remote user joined: $uid")
            _remoteUsers.value = _remoteUsers.value + uid
            onCallEventListener?.onRemoteUserJoined(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "Remote user offline: $uid, reason: $reason")
            _remoteUsers.value = _remoteUsers.value - uid
            onCallEventListener?.onRemoteUserLeft(uid)

            // If no more remote users, call might be ended
            if (_remoteUsers.value.isEmpty()) {
                onCallEventListener?.onCallEnded()
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(TAG, "Left channel, duration: ${stats?.totalDuration}s")
            _callState.value = CallState.Ended
            _remoteUsers.value = emptySet()
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora error: $err")
            val errorMessage = when (err) {
                ErrorCode.ERR_INVALID_TOKEN -> "Invalid token"
                ErrorCode.ERR_TOKEN_EXPIRED -> "Token expired"
                ErrorCode.ERR_NOT_INITIALIZED -> "Not initialized"
                ErrorCode.ERR_INVALID_CHANNEL_NAME -> "Invalid channel name"
                else -> "Error code: $err"
            }
            _callState.value = CallState.Error(errorMessage)
            onCallEventListener?.onError(err, errorMessage)
        }

        override fun onConnectionLost() {
            Log.w(TAG, "Connection lost")
            _callState.value = CallState.Reconnecting
            onCallEventListener?.onConnectionLost()
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d(TAG, "Connection state changed: $state, reason: $reason")
            when (state) {
                Constants.CONNECTION_STATE_CONNECTED -> {
                    _callState.value = CallState.Connected
                }
                Constants.CONNECTION_STATE_CONNECTING -> {
                    _callState.value = CallState.Connecting
                }
                Constants.CONNECTION_STATE_RECONNECTING -> {
                    _callState.value = CallState.Reconnecting
                }
                Constants.CONNECTION_STATE_DISCONNECTED -> {
                    _callState.value = CallState.Ended
                }
                Constants.CONNECTION_STATE_FAILED -> {
                    _callState.value = CallState.Error("Connection failed")
                }
            }
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            Log.d(TAG, "Remote audio state changed: uid=$uid, state=$state")
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            Log.d(TAG, "Remote video state changed: uid=$uid, state=$state")
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            speakers?.forEach { speaker ->
                if (speaker.volume > 0) {
                    onCallEventListener?.onUserSpeaking(speaker.uid, speaker.volume)
                }
            }
        }

        override fun onRtcStats(stats: RtcStats?) {
            stats?.let {
                _callDuration.value = it.totalDuration.toLong() * 1000
            }
        }

        override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
            onCallEventListener?.onNetworkQuality(uid, txQuality, rxQuality)
        }
    }

    /**
     * Call state
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
     * Call event listener interface
     */
    interface CallEventListener {
        fun onCallConnected() {}
        fun onCallEnded() {}
        fun onRemoteUserJoined(uid: Int) {}
        fun onRemoteUserLeft(uid: Int) {}
        fun onError(code: Int, message: String) {}
        fun onConnectionLost() {}
        fun onUserSpeaking(uid: Int, volume: Int) {}
        fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {}
    }
}
