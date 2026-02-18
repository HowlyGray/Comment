package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageStatus
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

    @Query("DELETE FROM messages WHERE id IN (:messageIds)")
    suspend fun deleteMessages(messageIds: List<String>)

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

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id IN (:messageIds)")
    suspend fun updateMultipleStarredStatus(messageIds: List<String>, isStarred: Boolean)

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE conversationId IN (
            SELECT id FROM conversations WHERE archived = 1
        ) AND isRead = 0
    """)
    fun getUnreadArchivedMessagesCount(): Flow<Int>

    // ACK : mise à jour du statut d'un message
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    // ACK : marquer tous les messages d'un expéditeur comme DELIVERED dans une conversation
    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId != :currentUserId AND status = 'SENT'")
    suspend fun markMessagesAsDelivered(conversationId: String, currentUserId: String, status: MessageStatus = MessageStatus.DELIVERED)

    // ACK : marquer tous les messages d'une conversation comme READ
    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != 'READ'")
    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String, status: MessageStatus = MessageStatus.READ)

    // Éphémères : supprimer les messages expirés
    @Query("DELETE FROM messages WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredMessages(now: Long = System.currentTimeMillis())

    // Éphémères : récupérer les IDs des messages expirés (pour synchro Firebase)
    @Query("SELECT id FROM messages WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun getExpiredMessageIds(now: Long = System.currentTimeMillis()): List<String>
}
