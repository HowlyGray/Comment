package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationById(conversationId: String): Flow<Conversation?>

    @Query("SELECT * FROM conversations")
    suspend fun getAllConversationsSync(): List<Conversation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
