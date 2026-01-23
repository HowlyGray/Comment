package com.memoryshare.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.memoryshare.app.MemoryShareApplication
import com.memoryshare.app.data.model.User
import com.memoryshare.app.data.repository.UserRepository
import com.memoryshare.app.services.firebase.FirebaseAuthManager
import com.memoryshare.app.services.firebase.FirestoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Authentication ViewModel
 * Handles user authentication with Firebase
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoryShareApplication
    private val authManager: FirebaseAuthManager = app.firebaseAuthManager
    private val firestoreManager: FirestoreManager = app.firestoreManager

    // Auth state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Current user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        checkAuthState()
    }

    /**
     * Check current authentication state
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            val firebaseUser = authManager.getCurrentUser()
            if (firebaseUser != null) {
                loadUserProfile(firebaseUser.uid)
                _authState.value = AuthState.Authenticated
                app.onUserLoggedIn(firebaseUser.uid)
            } else {
                _authState.value = AuthState.NotAuthenticated
            }
        }
    }

    /**
     * Sign up with email and password
     */
    fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String
    ) {
        if (!validateInput(email, password, username, displayName)) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authManager.signUpWithEmail(email, password, displayName)

            result.fold(
                onSuccess = { firebaseUser ->
                    // Create user profile in Firestore
                    val user = User(
                        id = firebaseUser.uid,
                        username = username,
                        displayName = displayName,
                        email = email,
                        profilePictureUrl = firebaseUser.photoUrl?.toString()
                    )

                    val saveResult = firestoreManager.saveUser(user)
                    if (saveResult.isSuccess) {
                        // Also save to local database
                        val userDao = app.database.userDao()
                        userDao.insertUser(user)

                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated
                        app.onUserLoggedIn(firebaseUser.uid)
                    } else {
                        _error.value = "Failed to save user profile"
                    }
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Sign up failed"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email and password are required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authManager.signInWithEmail(email, password)

            result.fold(
                onSuccess = { firebaseUser ->
                    loadUserProfile(firebaseUser.uid)
                    _authState.value = AuthState.Authenticated
                    app.onUserLoggedIn(firebaseUser.uid)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Sign in failed"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            app.onUserLoggedOut()
            _currentUser.value = null
            _authState.value = AuthState.NotAuthenticated
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _error.value = "Email is required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authManager.sendPasswordResetEmail(email)

            result.fold(
                onSuccess = {
                    _error.value = "Password reset email sent"
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to send reset email"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        profilePictureUrl: String? = null
    ) {
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Update Firebase Auth profile
            if (displayName != null || profilePictureUrl != null) {
                authManager.updateProfile(displayName, profilePictureUrl)
            }

            // Update Firestore profile
            val updatedUser = user.copy(
                displayName = displayName ?: user.displayName,
                bio = bio ?: user.bio,
                profilePictureUrl = profilePictureUrl ?: user.profilePictureUrl
            )

            val result = firestoreManager.saveUser(updatedUser)

            result.fold(
                onSuccess = {
                    // Update local database
                    app.database.userDao().updateUser(updatedUser)
                    _currentUser.value = updatedUser
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to update profile"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Delete account
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authManager.deleteAccount()

            result.fold(
                onSuccess = {
                    _currentUser.value?.let { user ->
                        app.database.userDao().deleteUser(user)
                    }
                    app.onUserLoggedOut()
                    _currentUser.value = null
                    _authState.value = AuthState.NotAuthenticated
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete account"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Load user profile from Firestore
     */
    private suspend fun loadUserProfile(userId: String) {
        val result = firestoreManager.getUser(userId)

        result.fold(
            onSuccess = { user ->
                if (user != null) {
                    _currentUser.value = user
                    // Cache in local database
                    app.database.userDao().insertUser(user)
                }
            },
            onFailure = { exception ->
                _error.value = exception.message ?: "Failed to load profile"
            }
        )
    }

    /**
     * Validate input
     */
    private fun validateInput(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Boolean {
        return when {
            email.isBlank() -> {
                _error.value = "Email is required"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _error.value = "Invalid email format"
                false
            }
            password.length < 6 -> {
                _error.value = "Password must be at least 6 characters"
                false
            }
            username.isBlank() -> {
                _error.value = "Username is required"
                false
            }
            username.length < 3 -> {
                _error.value = "Username must be at least 3 characters"
                false
            }
            displayName.isBlank() -> {
                _error.value = "Display name is required"
                false
            }
            else -> true
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean = authManager.isSignedIn()

    /**
     * Get current Firebase user
     */
    fun getFirebaseUser(): FirebaseUser? = authManager.getCurrentUser()

    /**
     * Auth state
     */
    sealed class AuthState {
        data object Loading : AuthState()
        data object NotAuthenticated : AuthState()
        data object Authenticated : AuthState()
    }
}
