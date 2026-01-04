package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.UserFollow
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFollowDao {
    @Query("SELECT * FROM user_follows WHERE followerId = :userId")
    fun getFollowing(userId: String): Flow<List<UserFollow>>

    @Query("SELECT * FROM user_follows WHERE followingId = :userId")
    fun getFollowers(userId: String): Flow<List<UserFollow>>

    @Query("SELECT COUNT(*) FROM user_follows WHERE followerId = :userId")
    fun getFollowingCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_follows WHERE followingId = :userId")
    fun getFollowersCount(userId: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM user_follows WHERE followerId = :followerId AND followingId = :followingId)")
    fun isFollowing(followerId: String, followingId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(userFollow: UserFollow)

    @Query("DELETE FROM user_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)

    @Query("DELETE FROM user_follows WHERE followerId = :userId OR followingId = :userId")
    suspend fun deleteAllFollowsForUser(userId: String)
}
