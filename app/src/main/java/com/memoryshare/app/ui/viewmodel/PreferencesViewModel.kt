package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.memoryshare.app.data.local.PreferencesManager
import com.memoryshare.app.data.model.PostVisibility
import com.memoryshare.app.data.model.PrivacySettings
import com.memoryshare.app.services.realtime.PresenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesViewModel(
    private val preferencesManager: PreferencesManager,
    private val presenceManager: PresenceManager? = null
) : ViewModel() {

    private val _fabOnLeft = MutableStateFlow(preferencesManager.isFabOnLeft())
    val fabOnLeft: StateFlow<Boolean> = _fabOnLeft.asStateFlow()

    private val _defaultPostVisibility = MutableStateFlow(
        PostVisibility.valueOf(preferencesManager.getDefaultPostVisibility())
    )
    val defaultPostVisibility: StateFlow<PostVisibility> = _defaultPostVisibility.asStateFlow()

    private val _privacySettings = MutableStateFlow(preferencesManager.getPrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()

    fun setFabOnLeft(onLeft: Boolean) {
        preferencesManager.setFabOnLeft(onLeft)
        _fabOnLeft.value = onLeft
    }

    fun setDefaultPostVisibility(visibility: PostVisibility) {
        preferencesManager.setDefaultPostVisibility(visibility.name)
        _defaultPostVisibility.value = visibility
    }

    fun savePrivacySettings(settings: PrivacySettings) {
        preferencesManager.savePrivacySettings(settings)
        _privacySettings.value = settings
        // Synchronise avec le PresenceManager pour que le statut en ligne respecte les nouveaux réglages
        presenceManager?.updatePrivacySettings(settings)
    }
}
