package com.memoryshare.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.memoryshare.app.data.model.Media
import com.memoryshare.app.data.model.MediaQuality
import com.memoryshare.app.data.model.MediaSourceType
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.PermissionLevel
import com.memoryshare.app.data.model.SharedSpace
import com.memoryshare.app.data.model.SharedSpacePermission
import com.memoryshare.app.data.repository.SharedSpaceRepository
import com.memoryshare.app.utils.FirebaseManager
import com.memoryshare.app.utils.FirebaseStorageManager
import com.memoryshare.app.utils.MediaSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SharedSpaceViewModel(
    private val repository: SharedSpaceRepository,
    private val mediaSyncManager: MediaSyncManager? = null
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
    private var spacesObserver: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    companion object {
        private const val TAG = "SharedSpaceViewModel"
    }

    init {
        observeAuthState()
        loadSpaces()
    }

    private fun loadSpaces() {
        viewModelScope.launch {
            repository.getAllSharedSpaces().collect { spaces ->
                _spaces.value = spaces
            }
        }
    }

    /**
     * Observe l'état d'authentification pour démarrer la synchronisation
     * dès que l'utilisateur est connecté (même si le ViewModel a été créé avant l'auth)
     */
    private fun observeAuthState() {
        authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                Log.d(TAG, "Auth state: user authenticated (${user.uid}), starting sync")
                startSyncing()
            } else {
                spacesObserver?.remove()
                spacesObserver = null
                Log.d(TAG, "Auth state: user signed out, stopped real-time sync")
            }
        }
        FirebaseManager.auth.addAuthStateListener(authListener!!)
    }

    /**
     * Appelé depuis l'UI pour garantir que la synchronisation est active
     * quand l'écran Souvenirs est affiché et l'utilisateur est connecté.
     * Accepte un userId explicite pour les cas où FirebaseAuth.currentUser est null
     * mais l'utilisateur est connecté au niveau de l'app (profil local).
     */
    fun ensureSyncing(userId: String? = null) {
        if (spacesObserver == null) {
            val uid = userId ?: FirebaseManager.getCurrentUserId()
            if (uid != null) {
                Log.d(TAG, "ensureSyncing: no active observer, starting sync for user: $uid")
                startSyncingForUser(uid)
            } else {
                Log.d(TAG, "ensureSyncing: no active observer and no userId available")
            }
        }
    }

    /**
     * Démarre la synchronisation via l'état d'authentification Firebase
     */
    private fun startSyncing() {
        FirebaseManager.getCurrentUserId()?.let { userId ->
            startSyncingForUser(userId)
        }
    }

    /**
     * Démarre la synchronisation en temps réel des dossiers pour un utilisateur donné
     */
    private fun startSyncingForUser(userId: String) {
        spacesObserver?.remove()
        spacesObserver = repository.startObservingUserSpaces(userId)
        Log.d(TAG, "Started real-time sync for user: $userId")
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
     * @param quality Qualité du média (HD ou SD) pour la compression
     */
    fun addMediaWithUpload(
        spaceId: String,
        uploaderId: String,
        mediaUri: Uri?,
        mediaUrl: String,
        type: MediaType,
        quality: MediaQuality = MediaQuality.SD,
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
                    // Utiliser MediaSyncManager si disponible pour compression et cache local
                    if (mediaSyncManager != null) {
                        Log.d(TAG, "Using MediaSyncManager for shared space media...")
                        _uploadProgress.value = 0f
                        _uploadError.value = null

                        // Préparer le média (compression + stockage local)
                        val prepareResult = mediaSyncManager.prepareMediaForUpload(
                            uri = mediaUri,
                            type = type,
                            quality = quality,
                            sourceType = MediaSourceType.SHARED_SPACE,
                            sourceId = spaceId,
                            uploadedBy = uploaderId
                        )

                        if (prepareResult.isFailure) {
                            val error = prepareResult.exceptionOrNull()?.message ?: "Erreur de préparation du média"
                            _uploadError.value = error
                            onError(error)
                            return@launch
                        }

                        val mediaCache = prepareResult.getOrThrow()
                        _uploadProgress.value = 0.5f

                        // Upload vers Firebase
                        val uploadResult = mediaSyncManager.uploadMedia(mediaCache)

                        if (uploadResult.isFailure) {
                            val error = uploadResult.exceptionOrNull()?.message ?: "Erreur d'upload"
                            _uploadError.value = error
                            onError(error)
                            return@launch
                        }

                        _uploadProgress.value = 1f
                        val firebaseUrl = uploadResult.getOrThrow()
                        Log.d(TAG, "Media uploaded successfully with caching: $firebaseUrl")
                        _uploadProgress.value = null
                        firebaseUrl
                    } else {
                        // Fallback: utiliser l'ancien système sans cache
                        Log.d(TAG, "MediaSyncManager not available, using legacy upload...")
                        val uploadResult = uploadMedia(mediaUri, type)
                        val url = uploadResult.getOrElse {
                            onError("Erreur d'upload: ${it.message}")
                            return@launch
                        }
                        url
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
                _uploadError.value = e.message
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

    fun updateSpace(space: SharedSpace) {
        viewModelScope.launch {
            repository.updateSharedSpace(space)
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

    override fun onCleared() {
        super.onCleared()
        spacesObserver?.remove()
        authListener?.let { FirebaseManager.auth.removeAuthStateListener(it) }
    }
}
