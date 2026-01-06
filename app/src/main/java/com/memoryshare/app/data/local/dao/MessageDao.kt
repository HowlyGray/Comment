package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageById(messageId: String): Flow<Message?>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageByIdSync(messageId: String): Message?

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND (senderId LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')")
    fun searchInConversation(conversationId: String, query: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId")
    suspend fun markConversationAsRead(conversationId: String)

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getAllStarredMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessagesByConversation(conversationId: String): Flow<List<Message>>

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun updateStarredStatus(messageId: String, isStarred: Boolean)
}
