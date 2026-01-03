package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.memoryshare.app.data.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _fabOnLeft = MutableStateFlow(preferencesManager.isFabOnLeft())
    val fabOnLeft: StateFlow<Boolean> = _fabOnLeft.asStateFlow()

    fun setFabOnLeft(onLeft: Boolean) {
        preferencesManager.setFabOnLeft(onLeft)
        _fabOnLeft.value = onLeft
    }
}
