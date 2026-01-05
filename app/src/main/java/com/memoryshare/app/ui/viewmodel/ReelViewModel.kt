package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Reel
import com.memoryshare.app.data.repository.ReelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReelViewModel(private val repository: ReelRepository) : ViewModel() {

    private val _reels = MutableStateFlow<List<Reel>>(emptyList())
    val reels: StateFlow<List<Reel>> = _reels.asStateFlow()

    private val _currentReelIndex = MutableStateFlow(0)
    val currentReelIndex: StateFlow<Int> = _currentReelIndex.asStateFlow()

    init {
        loadReels()
    }

    private fun loadReels() {
        viewModelScope.launch {
            repository.getAllReels().collect { reels ->
                _reels.value = reels
            }
        }
    }

    fun createReel(
        authorId: String,
        videoUrl: String,
        thumbnailUrl: String? = null,
        caption: String? = null,
        duration: Long,
        audioUrl: String? = null,
        audioName: String? = null
    ) {
        viewModelScope.launch {
            repository.createReel(authorId, videoUrl, thumbnailUrl, caption, duration, audioUrl, audioName)
        }
    }

    fun toggleLike(reel: Reel) {
        viewModelScope.launch {
            repository.toggleLike(reel)
        }
    }

    fun incrementViewCount(reel: Reel) {
        viewModelScope.launch {
            repository.incrementViewCount(reel)
        }
    }

    fun addComment(reelId: String, authorId: String, content: String) {
        viewModelScope.launch {
            repository.addComment(reelId, authorId, content)
        }
    }

    fun setCurrentReelIndex(index: Int) {
        _currentReelIndex.value = index
    }

    fun deleteReel(reel: Reel) {
        viewModelScope.launch {
            repository.deleteReel(reel)
        }
    }
}
