package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.UserDao
import com.memoryshare.app.data.local.dao.UserFollowDao
import com.memoryshare.app.data.model.User
import com.memoryshare.app.data.model.UserFollow
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(
    private val userDao: UserDao,
    private val userFollowDao: UserFollowDao
) {

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
        userFollowDao.insertFollow(userFollow)
    }

    suspend fun unfollowUser(followerId: String, followingId: String) {
        userFollowDao.deleteFollow(followerId, followingId)
    }
}
