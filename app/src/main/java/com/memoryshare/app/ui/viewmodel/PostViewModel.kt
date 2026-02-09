package com.memoryshare.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Comment
import com.memoryshare.app.data.model.MediaQuality
import com.memoryshare.app.data.model.MediaSourceType
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.Post
import com.memoryshare.app.data.model.PostMediaType
import com.memoryshare.app.data.repository.PostRepository
import com.memoryshare.app.utils.FirebaseStorageManager
import com.memoryshare.app.utils.MediaSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: PostRepository,
    private val mediaSyncManager: MediaSyncManager? = null
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _currentPost = MutableStateFlow<Post?>(null)
    val currentPost: StateFlow<Post?> = _currentPost.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    private val _followingPosts = MutableStateFlow<List<Post>>(emptyList())
    val followingPosts: StateFlow<List<Post>> = _followingPosts.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val storageManager = FirebaseStorageManager()

    companion object {
        private const val TAG = "PostViewModel"
    }

    init {
        syncAndLoadPosts()
    }

    /**
     * Synchronise les posts publics depuis Firebase puis charge le feed local
     */
    private fun syncAndLoadPosts() {
        viewModelScope.launch {
            // D'abord synchroniser les posts publics depuis Firebase
            try {
                repository.syncPostsFromFirebase()
                Log.d(TAG, "Firebase posts synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync posts from Firebase", e)
            }
            // Puis observer les posts locaux (incluant ceux synchronisés)
            repository.getPublicPosts().collect { posts ->
                _posts.value = posts
            }
        }
    }

    /**
     * Rafraîchit les posts depuis Firebase
     */
    fun refreshPosts() {
        syncAndLoadPosts()
    }

    fun loadFeedPosts(currentUserId: String) {
        viewModelScope.launch {
            // Synchroniser d'abord
            try {
                repository.syncPostsFromFirebase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync posts from Firebase", e)
            }
            repository.getFeedPosts(currentUserId).collect { posts ->
                _posts.value = posts
            }
        }
    }

    fun loadFollowingPosts(userId: String) {
        viewModelScope.launch {
            // Synchroniser d'abord
            try {
                repository.syncPostsFromFirebase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync posts from Firebase", e)
            }
            repository.getFollowingPosts(userId).collect { posts ->
                _followingPosts.value = posts
            }
        }
    }

    fun loadPost(postId: String) {
        viewModelScope.launch {
            repository.getPostById(postId).collect { post ->
                _currentPost.value = post
            }
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            repository.getCommentsByPost(postId).collect { comments ->
                _comments.value = comments
            }
        }
    }

    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            repository.getPostsByUser(userId).collect { posts ->
                _userPosts.value = posts
            }
        }
    }

    /**
     * Upload un média vers Firebase Storage
     */
    suspend fun uploadMedia(uri: Uri, mediaType: PostMediaType): Result<String> {
        return try {
            _uploadProgress.value = 0f
            _uploadError.value = null

            val result = when (mediaType) {
                PostMediaType.IMAGE -> storageManager.uploadImage(uri)
                PostMediaType.VIDEO -> storageManager.uploadVideo(uri)
                PostMediaType.AUDIO -> storageManager.uploadAudio(uri)
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
     * Crée un post avec upload automatique du média si c'est un URI local
     * @param quality Qualité du média (HD ou SD) pour la compression
     * @param onSuccess Callback appelé avec l'URL finale du média uploadé
     */
    fun createPostWithMedia(
        authorId: String,
        mediaUri: Uri?,
        mediaUrl: String,
        mediaType: PostMediaType,
        quality: MediaQuality = MediaQuality.SD,
        caption: String? = null,
        thumbnailUrl: String? = null,
        visibility: com.memoryshare.app.data.model.PostVisibility = com.memoryshare.app.data.model.PostVisibility.PUBLIC,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // Vérifier si c'est un URI local ou une URL web
                val finalUrl = if (mediaUri != null && mediaUrl.startsWith("content://")) {
                    // Utiliser MediaSyncManager si disponible pour compression et cache local
                    if (mediaSyncManager != null) {
                        Log.d(TAG, "Using MediaSyncManager for local caching and compression...")
                        _uploadProgress.value = 0f
                        _uploadError.value = null

                        // Convertir PostMediaType vers MediaType
                        val cacheMediaType = when (mediaType) {
                            PostMediaType.IMAGE -> MediaType.IMAGE
                            PostMediaType.VIDEO -> MediaType.VIDEO
                            PostMediaType.AUDIO -> MediaType.AUDIO
                        }

                        // Préparer le média (compression + stockage local)
                        val prepareResult = mediaSyncManager.prepareMediaForUpload(
                            uri = mediaUri,
                            type = cacheMediaType,
                            quality = quality,
                            sourceType = MediaSourceType.POST,
                            sourceId = "", // Will be set after post creation
                            uploadedBy = authorId
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
                        val uploadResult = uploadMedia(mediaUri, mediaType)
                        uploadResult.getOrElse {
                            onError("Erreur d'upload: ${it.message}")
                            return@launch
                        }
                    }
                } else {
                    // C'est déjà une URL web, on l'utilise directement
                    mediaUrl
                }

                // Créer le post avec l'URL Firebase ou l'URL web
                Log.d(TAG, "Creating post with URL: $finalUrl")
                repository.createPost(authorId, listOf(finalUrl), mediaType, caption, thumbnailUrl, visibility)
                onSuccess(finalUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating post", e)
                _uploadError.value = e.message
                onError(e.message ?: "Erreur de création du post")
            }
        }
    }

    fun createPost(
        authorId: String,
        mediaUrls: List<String>,
        mediaType: PostMediaType,
        caption: String? = null,
        thumbnailUrl: String? = null,
        visibility: com.memoryshare.app.data.model.PostVisibility = com.memoryshare.app.data.model.PostVisibility.PUBLIC
    ) {
        viewModelScope.launch {
            repository.createPost(authorId, mediaUrls, mediaType, caption, thumbnailUrl, visibility)
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            repository.toggleLike(post)
        }
    }

    fun addComment(postId: String, authorId: String, content: String) {
        viewModelScope.launch {
            repository.addComment(postId, authorId, content)
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            repository.deletePost(post)
        }
    }

    fun updatePost(post: Post) {
        viewModelScope.launch {
            repository.updatePost(post)
        }
    }

    fun updatePostVisibility(post: Post, visibility: com.memoryshare.app.data.model.PostVisibility) {
        viewModelScope.launch {
            val updatedPost = post.copy(visibility = visibility)
            repository.updatePost(updatedPost)
        }
    }

    fun updatePostCaption(post: Post, caption: String) {
        viewModelScope.launch {
            val updatedPost = post.copy(caption = caption)
            repository.updatePost(updatedPost)
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            repository.deleteComment(comment)
        }
    }
}
