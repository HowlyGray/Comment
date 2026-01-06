package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Call
import com.memoryshare.app.data.model.CallStatus
import com.memoryshare.app.data.model.CallType
import com.memoryshare.app.data.repository.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallViewModel(
    private val repository: CallRepository
) : ViewModel() {

    private val _calls = MutableStateFlow<List<Call>>(emptyList())
    val calls: StateFlow<List<Call>> = _calls.asStateFlow()

    private val _currentCall = MutableStateFlow<Call?>(null)
    val currentCall: StateFlow<Call?> = _currentCall.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    init {
        loadCalls()
    }

    private fun loadCalls() {
        viewModelScope.launch {
            repository.getAllCalls().collect { calls ->
                _calls.value = calls
            }
        }
    }

    fun loadCallsByUser(userId: String) {
        viewModelScope.launch {
            repository.getCallsByUser(userId).collect { calls ->
                _calls.value = calls
            }
        }
    }

    fun startCall(userId: String, type: CallType, isIncoming: Boolean = false) {
        viewModelScope.launch {
            val call = repository.createCall(userId, type, isIncoming)
            _currentCall.value = call
            _callDuration.value = 0L
        }
    }

    fun endCall() {
        viewModelScope.launch {
            _currentCall.value?.let { call ->
                repository.endCall(call.id, _callDuration.value)
                _currentCall.value = null
                _callDuration.value = 0L
            }
        }
    }

    fun updateCallStatus(status: CallStatus) {
        viewModelScope.launch {
            _currentCall.value?.let { call ->
                repository.updateCallStatus(call.id, status, _callDuration.value)
            }
        }
    }

    fun incrementCallDuration() {
        _callDuration.value += 1
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleVideo() {
        _isVideoEnabled.value = !_isVideoEnabled.value
    }

    fun deleteCall(call: Call) {
        viewModelScope.launch {
            repository.deleteCall(call)
        }
    }

    fun deleteAllCalls() {
        viewModelScope.launch {
            repository.deleteAllCalls()
        }
    }
}
