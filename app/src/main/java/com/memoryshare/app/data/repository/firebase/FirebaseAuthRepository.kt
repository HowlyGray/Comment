package com.memoryshare.app.data.repository.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.memoryshare.app.utils.FirebaseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pour gérer l'authentification Firebase
 */
class FirebaseAuthRepository {
    private val auth = FirebaseManager.auth

    companion object {
        private const val TAG = "FirebaseAuthRepository"
    }

    /**
     * Flow pour observer les changements d'état d'authentification
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    /**
     * Inscription avec email et mot de passe
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")

            // Mettre à jour le profil avec le nom d'affichage
            displayName?.let {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(it)
                    .build()
                user.updateProfile(profileUpdates).await()
            }

            Log.d(TAG, "User signed up successfully: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up with email", e)
            Result.failure(e)
        }
    }

    /**
     * Connexion avec email et mot de passe
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Log.d(TAG, "User signed in successfully: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with email", e)
            Result.failure(e)
        }
    }

    /**
     * Connexion anonyme
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Anonymous sign in failed")
            Log.d(TAG, "User signed in anonymously: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in anonymously", e)
            Result.failure(e)
        }
    }

    /**
     * Déconnexion
     */
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    /**
     * Récupère l'utilisateur actuel
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Envoie un email de réinitialisation du mot de passe
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending password reset email", e)
            Result.failure(e)
        }
    }

    /**
     * Met à jour le profil de l'utilisateur
     */
    suspend fun updateUserProfile(displayName: String?, photoUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user signed in")
            val profileUpdates = UserProfileChangeRequest.Builder()
                .apply {
                    displayName?.let { setDisplayName(it) }
                    photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
                }
                .build()
            user.updateProfile(profileUpdates).await()
            Log.d(TAG, "User profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime le compte utilisateur
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user signed in")
            user.delete().await()
            Log.d(TAG, "User account deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account", e)
            Result.failure(e)
        }
    }
}
