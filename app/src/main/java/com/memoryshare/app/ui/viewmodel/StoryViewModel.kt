package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Story
import com.memoryshare.app.data.model.StoryMediaType
import com.memoryshare.app.data.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoryViewModel(private val repository: StoryRepository) : ViewModel() {

    private val _activeStories = MutableStateFlow<List<Story>>(emptyList())
    val activeStories: StateFlow<List<Story>> = _activeStories.asStateFlow()

    private val _userStories = MutableStateFlow<List<Story>>(emptyList())
    val userStories: StateFlow<List<Story>> = _userStories.asStateFlow()

    private val _authorsWithStories = MutableStateFlow<List<String>>(emptyList())
    val authorsWithStories: StateFlow<List<String>> = _authorsWithStories.asStateFlow()

    init {
        loadActiveStories()
        loadAuthorsWithStories()
    }

    private fun loadActiveStories() {
        viewModelScope.launch {
            repository.getActiveStories().collect { stories ->
                _activeStories.value = stories
            }
        }
    }

    private fun loadAuthorsWithStories() {
        viewModelScope.launch {
            repository.getAuthorsWithActiveStories().collect { authors ->
                _authorsWithStories.value = authors
            }
        }
    }

    fun loadStoriesByUser(userId: String) {
        viewModelScope.launch {
            repository.getStoriesByUser(userId).collect { stories ->
                _userStories.value = stories
            }
        }
    }

    fun createStory(
        authorId: String,
        mediaUrl: String,
        mediaType: StoryMediaType,
        thumbnailUrl: String? = null,
        caption: String? = null,
        duration: Long? = null
    ) {
        viewModelScope.launch {
            repository.createStory(authorId, mediaUrl, mediaType, thumbnailUrl, caption, duration)
        }
    }

    fun markAsViewed(story: Story, userId: String) {
        viewModelScope.launch {
            repository.markAsViewed(story, userId)
        }
    }

    fun deleteStory(story: Story) {
        viewModelScope.launch {
            repository.deleteStory(story)
        }
    }

    fun cleanupExpiredStories() {
        viewModelScope.launch {
            repository.deleteExpiredStories()
        }
    }
}
