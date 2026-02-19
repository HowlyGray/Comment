package com.memoryshare.app.data.repository

import android.util.Log
import com.memoryshare.app.data.local.dao.UserDao
import com.memoryshare.app.data.local.dao.UserFollowDao
import com.memoryshare.app.data.model.User
import com.memoryshare.app.data.model.UserFollow
import com.memoryshare.app.utils.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val userFollowDao: UserFollowDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore = FirebaseManager.firestore

    companion object {
        private const val TAG = "UserRepository"
    }

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    fun getUserById(userId: String): Flow<User?> = userDao.getUserById(userId)

    fun getUsersByIds(userIds: List<String>): Flow<List<User>> = userDao.getUsersByIds(userIds)

    fun searchUsers(query: String): Flow<List<User>> = userDao.searchUsers(query)

    fun searchUsersByAll(query: String): Flow<List<User>> = userDao.searchUsersByAll(query)

    /**
     * Search users on Firebase by exact email or username, then cache results locally.
     * Returns the list of matching users found on Firebase.
     */
    suspend fun searchUsersOnFirebase(query: String): List<User> {
        val results = mutableMapOf<String, User>()
        try {
            // Search by exact email
            val emailSnapshot = firestore.collection(FirebaseManager.Collections.USERS)
                .whereEqualTo("email", query)
                .get()
                .await()
            emailSnapshot.documents.mapNotNull { it.toObject(User::class.java) }
                .forEach { results[it.id] = it }

            // Search by exact username
            val usernameSnapshot = firestore.collection(FirebaseManager.Collections.USERS)
                .whereEqualTo("username", query)
                .get()
                .await()
            usernameSnapshot.documents.mapNotNull { it.toObject(User::class.java) }
                .forEach { results[it.id] = it }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search users on Firebase", e)
        }
        // Cache found users locally
        val users = results.values.toList()
        users.forEach { userDao.insertUser(it) }
        return users
    }

    suspend fun createUser(
        username: String,
        displayName: String,
        email: String,
        profilePictureUrl: String? = null,
        bio: String? = null
    ): User {
        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            displayName = displayName,
            email = email,
            profilePictureUrl = profilePictureUrl,
            bio = bio
        )
        // Sauvegarder localement
        userDao.insertUser(user)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.USERS,
                documentId = user.id,
                data = user
            ).onSuccess {
                Log.d(TAG, "User synced to Firebase: ${user.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync user to Firebase: ${user.id}", e)
            }
        }

        return user
    }

    suspend fun updateUser(user: User) {
        // Mettre à jour localement
        userDao.updateUser(user)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.USERS,
                documentId = user.id,
                data = user
            ).onSuccess {
                Log.d(TAG, "User update synced to Firebase: ${user.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync user update to Firebase: ${user.id}", e)
            }
        }
    }

    suspend fun deleteUser(user: User) {
        // Supprimer localement
        userDao.deleteUser(user)

        // Supprimer de Firebase
        scope.launch {
            FirebaseManager.deleteDocument(
                collection = FirebaseManager.Collections.USERS,
                documentId = user.id
            ).onSuccess {
                Log.d(TAG, "User deleted from Firebase: ${user.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete user from Firebase: ${user.id}", e)
            }
        }
    }

    // Follow/Unfollow methods
    fun getFollowing(userId: String): Flow<List<UserFollow>> =
        userFollowDao.getFollowing(userId)

    fun getFollowers(userId: String): Flow<List<UserFollow>> =
        userFollowDao.getFollowers(userId)

    fun getFollowingCount(userId: String): Flow<Int> =
        userFollowDao.getFollowingCount(userId)

    fun getFollowersCount(userId: String): Flow<Int> =
        userFollowDao.getFollowersCount(userId)

    fun isFollowing(followerId: String, followingId: String): Flow<Boolean> =
        userFollowDao.isFollowing(followerId, followingId)

    suspend fun followUser(followerId: String, followingId: String) {
        val userFollow = UserFollow(
            id = UUID.randomUUID().toString(),
            followerId = followerId,
            followingId = followingId
        )
        // Sauvegarder localement
        userFollowDao.insertFollow(userFollow)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.USER_FOLLOWS,
                documentId = userFollow.id,
                data = userFollow
            ).onSuccess {
                Log.d(TAG, "Follow synced to Firebase: ${userFollow.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync follow to Firebase: ${userFollow.id}", e)
            }
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String) {
        // Supprimer localement
        userFollowDao.deleteFollow(followerId, followingId)

        // Note: Pour supprimer de Firebase, on devrait d'abord récupérer l'ID du document
        // Pour simplifier, on suppose qu'on a une fonction pour récupérer le follow
        scope.launch {
            try {
                // Cette partie nécessiterait une query pour trouver le document
                // Pour l'instant, on log l'action
                Log.d(TAG, "Unfollow action - local deletion completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error during unfollow sync", e)
            }
        }
    }

    /**
     * Synchronise les utilisateurs depuis Firebase vers la base de données locale
     */
    suspend fun syncUsersFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.USERS,
            clazz = User::class.java
        ).onSuccess { users ->
            users.forEach { user ->
                userDao.insertUser(user)
            }
            Log.d(TAG, "Synced ${users.size} users from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync users from Firebase", e)
        }
    }

    /**
     * Synchronise les relations de follow depuis Firebase vers la base de données locale
     */
    suspend fun syncFollowsFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.USER_FOLLOWS,
            clazz = UserFollow::class.java
        ).onSuccess { follows ->
            follows.forEach { follow ->
                userFollowDao.insertFollow(follow)
            }
            Log.d(TAG, "Synced ${follows.size} follow relationships from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync follow relationships from Firebase", e)
        }
    }
}
