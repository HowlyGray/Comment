package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Comment
import com.memoryshare.app.data.model.Post
import com.memoryshare.app.data.model.PostMediaType
import com.memoryshare.app.data.repository.PostRepository
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
