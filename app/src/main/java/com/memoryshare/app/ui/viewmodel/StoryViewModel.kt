package com.memoryshare.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Story
import com.memoryshare.app.data.model.StoryMediaType
import com.memoryshare.app.data.repository.StoryRepository
import com.memoryshare.app.utils.FirebaseStorageManager
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

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val storageManager = FirebaseStorageManager()

    companion object {
        private const val TAG = "StoryViewModel"
    }

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

    /**
     * Upload un média vers Firebase Storage
     */
    suspend fun uploadMedia(uri: Uri, mediaType: StoryMediaType): Result<String> {
        return try {
            _uploadProgress.value = 0f
            _uploadError.value = null

            val result = when (mediaType) {
                StoryMediaType.IMAGE -> storageManager.uploadImage(uri)
                StoryMediaType.VIDEO -> storageManager.uploadVideo(uri)
            }

            result.onSuccess {
                _uploadProgress.value = 1f
                Log.d(TAG, "Story media uploaded successfully: $it")
            }.onFailure { e ->
                _uploadError.value = e.message ?: "Erreur d'upload"
                Log.e(TAG, "Story media upload failed", e)
            }

            _uploadProgress.value = null
            result
        } catch (e: Exception) {
            _uploadProgress.value = null
            _uploadError.value = e.message ?: "Erreur d'upload"
            Log.e(TAG, "Story media upload error", e)
            Result.failure(e)
        }
    }

    /**
     * Crée une story avec upload automatique si c'est un URI local
     */
    fun createStoryWithMedia(
        authorId: String,
        mediaUri: Uri?,
        mediaUrl: String,
        mediaType: StoryMediaType,
        thumbnailUrl: String? = null,
        caption: String? = null,
        duration: Long? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // Vérifier si c'est un URI local ou une URL web
                val finalUrl = if (mediaUri != null && mediaUrl.startsWith("content://")) {
                    // Upload vers Firebase Storage
                    Log.d(TAG, "Uploading local story media to Firebase Storage...")
                    val uploadResult = uploadMedia(mediaUri, mediaType)
                    uploadResult.getOrElse {
                        onError("Erreur d'upload: ${it.message}")
                        return@launch
                    }
                } else {
                    // C'est déjà une URL web, on l'utilise directement
                    mediaUrl
                }

                // Créer la story avec l'URL Firebase ou l'URL web
                Log.d(TAG, "Creating story with URL: $finalUrl")
                repository.createStory(authorId, finalUrl, mediaType, thumbnailUrl, caption, duration)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating story", e)
                onError(e.message ?: "Erreur de création de la story")
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
