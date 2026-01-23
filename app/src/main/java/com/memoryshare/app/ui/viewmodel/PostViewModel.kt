package com.memoryshare.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Comment
import com.memoryshare.app.data.model.Post
import com.memoryshare.app.data.model.PostMediaType
import com.memoryshare.app.data.repository.PostRepository
import com.memoryshare.app.utils.FirebaseStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: PostRepository
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
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            repository.getAllPosts().collect { posts ->
                _posts.value = posts
            }
        }
    }

    fun loadFollowingPosts(userId: String) {
        viewModelScope.launch {
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
     */
    fun createPostWithMedia(
        authorId: String,
        mediaUri: Uri?,
        mediaUrl: String,
        mediaType: PostMediaType,
        caption: String? = null,
        thumbnailUrl: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // Vérifier si c'est un URI local ou une URL web
                val finalUrl = if (mediaUri != null && mediaUrl.startsWith("content://")) {
                    // Upload vers Firebase Storage
                    Log.d(TAG, "Uploading local media to Firebase Storage...")
                    val uploadResult = uploadMedia(mediaUri, mediaType)
                    uploadResult.getOrElse {
                        onError("Erreur d'upload: ${it.message}")
                        return@launch
                    }
                } else {
                    // C'est déjà une URL web, on l'utilise directement
                    mediaUrl
                }

                // Créer le post avec l'URL Firebase ou l'URL web
                Log.d(TAG, "Creating post with URL: $finalUrl")
                repository.createPost(authorId, listOf(finalUrl), mediaType, caption, thumbnailUrl)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating post", e)
                onError(e.message ?: "Erreur de création du post")
            }
        }
    }

    fun createPost(
        authorId: String,
        mediaUrls: List<String>,
        mediaType: PostMediaType,
        caption: String? = null,
        thumbnailUrl: String? = null
    ) {
        viewModelScope.launch {
            repository.createPost(authorId, mediaUrls, mediaType, caption, thumbnailUrl)
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

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            repository.deleteComment(comment)
        }
    }
}
