package com.memoryshare.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Media
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.PermissionLevel
import com.memoryshare.app.data.model.SharedSpace
import com.memoryshare.app.data.model.SharedSpacePermission
import com.memoryshare.app.data.repository.SharedSpaceRepository
import com.memoryshare.app.utils.FirebaseStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SharedSpaceViewModel(
    private val repository: SharedSpaceRepository
) : ViewModel() {

    private val _spaces = MutableStateFlow<List<SharedSpace>>(emptyList())
    val spaces: StateFlow<List<SharedSpace>> = _spaces.asStateFlow()

    private val _currentSpace = MutableStateFlow<SharedSpace?>(null)
    val currentSpace: StateFlow<SharedSpace?> = _currentSpace.asStateFlow()

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media: StateFlow<List<Media>> = _media.asStateFlow()

    private val _permissions = MutableStateFlow<List<SharedSpacePermission>>(emptyList())
    val permissions: StateFlow<List<SharedSpacePermission>> = _permissions.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val storageManager = FirebaseStorageManager()

    companion object {
        private const val TAG = "SharedSpaceViewModel"
    }

    init {
        loadSpaces()
    }

    private fun loadSpaces() {
        viewModelScope.launch {
            repository.getAllSharedSpaces().collect { spaces ->
                _spaces.value = spaces
            }
        }
    }

    fun loadSpace(spaceId: String) {
        viewModelScope.launch {
            repository.getSharedSpaceById(spaceId).collect { space ->
                _currentSpace.value = space
            }
        }
    }

    fun loadMedia(spaceId: String, type: MediaType? = null) {
        viewModelScope.launch {
            if (type != null) {
                repository.getMediaBySpaceAndType(spaceId, type).collect { media ->
                    _media.value = media
                }
            } else {
                repository.getMediaBySpace(spaceId).collect { media ->
                    _media.value = media
                }
            }
        }
    }

    fun createSpace(
        name: String,
        creatorId: String,
        description: String? = null,
        memberIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            repository.createSharedSpace(name, creatorId, description, memberIds)
        }
    }

    /**
     * Upload un média vers Firebase Storage
     */
    suspend fun uploadMedia(uri: Uri, mediaType: MediaType): Result<String> {
        return try {
            _uploadProgress.value = 0f
            _uploadError.value = null

            val result = when (mediaType) {
                MediaType.IMAGE -> storageManager.uploadImage(uri)
                MediaType.VIDEO -> storageManager.uploadVideo(uri)
                MediaType.AUDIO -> storageManager.uploadAudio(uri)
            }

            result.onSuccess {
                _uploadProgress.value = 1f
                Log.d(TAG, "Media uploaded successfully: $it")
            }.onFailure { e ->
                _uploadError.value = e.message ?: "Erreur d'upload"
                Log.e(TAG, "Media upload failed", e)
            }

            _uploadProgress.value = null
            result
        } catch (e: Exception) {
            _uploadProgress.value = null
            _uploadError.value = e.message ?: "Erreur d'upload"
            Log.e(TAG, "Media upload error", e)
            Result.failure(e)
        }
    }

    /**
     * Ajoute un média avec upload automatique si c'est un URI local
     */
    fun addMediaWithUpload(
        spaceId: String,
        uploaderId: String,
        mediaUri: Uri?,
        mediaUrl: String,
        type: MediaType,
        title: String? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        duration: Long? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // Vérifier si c'est un URI local ou une URL web
                val finalUrl = if (mediaUri != null && mediaUrl.startsWith("content://")) {
                    // Upload vers Firebase Storage
                    Log.d(TAG, "Uploading local media to Firebase Storage...")
                    val uploadResult = uploadMedia(mediaUri, type)
                    uploadResult.getOrElse {
                        onError("Erreur d'upload: ${it.message}")
                        return@launch
                    }
                } else {
                    // C'est déjà une URL web, on l'utilise directement
                    mediaUrl
                }

                // Ajouter le média avec l'URL Firebase ou l'URL web
                Log.d(TAG, "Adding media with URL: $finalUrl")
                repository.addMedia(spaceId, uploaderId, finalUrl, type, title, description, thumbnailUrl, duration)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding media", e)
                onError(e.message ?: "Erreur lors de l'ajout du média")
            }
        }
    }

    fun addMedia(
        spaceId: String,
        uploaderId: String,
        url: String,
        type: MediaType,
        title: String? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        duration: Long? = null
    ) {
        viewModelScope.launch {
            repository.addMedia(spaceId, uploaderId, url, type, title, description, thumbnailUrl, duration)
        }
    }

    fun addMember(spaceId: String, userId: String) {
        viewModelScope.launch {
            repository.addMemberToSpace(spaceId, userId)
        }
    }

    fun removeMember(spaceId: String, userId: String) {
        viewModelScope.launch {
            repository.removeMemberFromSpace(spaceId, userId)
        }
    }

    fun deleteMedia(media: Media) {
        viewModelScope.launch {
            repository.deleteMedia(media)
        }
    }

    fun deleteSpace(space: SharedSpace) {
        viewModelScope.launch {
            repository.deleteSharedSpace(space)
        }
    }

    fun loadPermissions(spaceId: String) {
        viewModelScope.launch {
            repository.getSpacePermissions(spaceId).collect { permissions ->
                _permissions.value = permissions
            }
        }
    }

    fun updatePermission(spaceId: String, userId: String, permissionLevel: PermissionLevel) {
        viewModelScope.launch {
            val permission = SharedSpacePermission(
                id = UUID.randomUUID().toString(),
                spaceId = spaceId,
                userId = userId,
                permission = permissionLevel
            )
            repository.updatePermission(permission)
        }
    }
}
