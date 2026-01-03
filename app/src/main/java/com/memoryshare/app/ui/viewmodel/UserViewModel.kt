package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.User
import com.memoryshare.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            repository.getAllUsers().collect { users ->
                _users.value = users
            }
        }
    }

    fun setCurrentUser(user: User) {
        _currentUser.value = user
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
