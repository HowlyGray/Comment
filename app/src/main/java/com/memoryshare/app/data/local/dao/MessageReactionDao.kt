package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.MessageReaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReactionDao {
    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId")
    fun getReactionsForMessage(messageId: String): Flow<List<MessageReaction>>

    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId AND userId = :userId")
    fun getUserReaction(messageId: String, userId: String): Flow<MessageReaction?>

    @Query("SELECT COUNT(*) FROM message_reactions WHERE messageId = :messageId AND emoji = :emoji")
    fun getReactionCount(messageId: String, emoji: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: MessageReaction)

    @Delete
    suspend fun deleteReaction(reaction: MessageReaction)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND userId = :userId")
    suspend fun deleteUserReaction(messageId: String, userId: String)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId")
    suspend fun deleteAllReactionsForMessage(messageId: String)
}
