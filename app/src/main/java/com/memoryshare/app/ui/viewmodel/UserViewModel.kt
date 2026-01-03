package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.local.PreferencesManager
import com.memoryshare.app.data.model.User
import com.memoryshare.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    init {
        loadUsers()
        restoreCurrentUser()
    }

    private fun restoreCurrentUser() {
        viewModelScope.launch {
            val savedUserId = preferencesManager.getCurrentUserId()
            if (savedUserId != null) {
                val user = repository.getUserById(savedUserId).firstOrNull()
                _currentUser.value = user
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            repository.getAllUsers().collect { users ->
                _users.value = users
            }
        }
    }

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
        preferencesManager.saveCurrentUserId(user?.id)
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
}
