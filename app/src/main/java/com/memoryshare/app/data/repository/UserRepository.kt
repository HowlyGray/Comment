package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.UserDao
import com.memoryshare.app.data.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(private val userDao: UserDao) {

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    fun getUserById(userId: String): Flow<User?> = userDao.getUserById(userId)

    fun getUsersByIds(userIds: List<String>): Flow<List<User>> = userDao.getUsersByIds(userIds)

    fun searchUsers(query: String): Flow<List<User>> = userDao.searchUsers(query)

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
        userDao.insertUser(user)
        return user
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }
}
