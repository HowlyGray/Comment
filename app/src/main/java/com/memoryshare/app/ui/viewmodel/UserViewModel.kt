package com.memoryshare.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.local.PreferencesManager
import com.memoryshare.app.data.model.User
import com.memoryshare.app.data.repository.MessageRepository
import com.memoryshare.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository,
    private val preferencesManager: PreferencesManager,
    private val messageRepository: MessageRepository? = null
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _followingUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followingUserIds: StateFlow<Set<String>> = _followingUserIds.asStateFlow()

    companion object {
        private const val TAG = "UserViewModel"
    }

    init {
        syncAndLoadUsers()
        restoreCurrentUser()
    }

    private fun restoreCurrentUser() {
        viewModelScope.launch {
            val savedUserId = preferencesManager.getCurrentUserId()
            if (savedUserId != null) {
                val user = repository.getUserById(savedUserId).firstOrNull()
                _currentUser.value = user
                loadFollowingUserIds(savedUserId)
            }
        }
    }

    private fun loadFollowingUserIds(userId: String) {
        viewModelScope.launch {
            repository.getFollowing(userId).collect { follows ->
                _followingUserIds.value = follows.map { it.followingId }.toSet()
            }
        }
    }

    /**
     * Synchronise les utilisateurs depuis Firebase puis observe la base locale
     */
    private fun syncAndLoadUsers() {
        viewModelScope.launch {
            // Synchroniser d'abord les utilisateurs depuis Firebase
            try {
                repository.syncUsersFromFirebase()
                Log.d(TAG, "Users synced from Firebase")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync users from Firebase", e)
            }
            // Puis observer les utilisateurs locaux
            repository.getAllUsers().collect { users ->
                _users.value = users
            }
        }
    }

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
        preferencesManager.saveCurrentUserId(user?.id)
        user?.id?.let { loadFollowingUserIds(it) }
    }

    fun loadUser(userId: String) {
        viewModelScope.launch {
            repository.getUserById(userId).collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            repository.searchUsers(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun createUser(
        username: String,
        displayName: String,
        email: String,
        profilePictureUrl: String? = null,
        bio: String? = null
    ) {
        viewModelScope.launch {
            val user = repository.createUser(username, displayName, email, profilePictureUrl, bio)
            _currentUser.value = user
            preferencesManager.saveCurrentUserId(user.id)
        }
    }

    fun addContact(
        username: String,
        displayName: String,
        email: String,
        profilePictureUrl: String? = null,
        bio: String? = null
    ) {
        viewModelScope.launch {
            repository.createUser(username, displayName, email, profilePictureUrl, bio)
            // Ne pas mettre à jour currentUser
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            repository.updateUser(user)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }
        }
    }

    // Follow/Unfollow methods
    fun loadFollowStats(userId: String) {
        viewModelScope.launch {
            repository.getFollowersCount(userId).collect { count ->
                _followersCount.value = count
            }
        }
        viewModelScope.launch {
            repository.getFollowingCount(userId).collect { count ->
                _followingCount.value = count
            }
        }
    }

    fun checkIsFollowing(followingId: String) {
        viewModelScope.launch {
            _currentUser.value?.let { currentUser ->
                repository.isFollowing(currentUser.id, followingId).collect { following ->
                    _isFollowing.value = following
                }
            }
        }
    }

    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            _currentUser.value?.let { currentUser ->
                val isCurrentlyFollowing = _followingUserIds.value.contains(userId)
                if (isCurrentlyFollowing) {
                    repository.unfollowUser(currentUser.id, userId)
                } else {
                    repository.followUser(currentUser.id, userId)
                    // Auto-create conversation when following a user
                    try {
                        messageRepository?.findOrCreateConversation(
                            participantIds = listOf(currentUser.id, userId)
                        )
                        Log.d(TAG, "Conversation created/found with followed user: $userId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create conversation with followed user", e)
                    }
                }
            }
        }
    }

    // Get user by ID as Flow
    fun getUserById(userId: String) = repository.getUserById(userId)
}
