package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Story
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getActiveStories(currentTime: Long = System.currentTimeMillis()): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE authorId = :userId AND expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getStoriesByUser(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    fun getStoryById(storyId: String): Flow<Story?>

    @Query("SELECT DISTINCT authorId FROM stories WHERE expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getAuthorsWithActiveStories(currentTime: Long = System.currentTimeMillis()): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Update
    suspend fun updateStory(story: Story)

    @Delete
    suspend fun deleteStory(story: Story)

    @Query("DELETE FROM stories WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredStories(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM stories WHERE authorId = :userId")
    suspend fun deleteStoriesByUser(userId: String)
}
