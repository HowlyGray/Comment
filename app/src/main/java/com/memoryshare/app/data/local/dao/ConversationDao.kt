package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE archived = 0 ORDER BY pinned DESC, pinnedAt DESC, lastMessageTime DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE archived = 1 ORDER BY lastMessageTime DESC")
    fun getArchivedConversations(): Flow<List<Conversation>>

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

    @Query("DELETE FROM conversations WHERE id IN (:conversationIds)")
    suspend fun deleteConversations(conversationIds: List<String>)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("UPDATE conversations SET archived = :archived WHERE id = :conversationId")
    suspend fun updateArchivedStatus(conversationId: String, archived: Boolean)

    @Query("UPDATE conversations SET archived = :archived WHERE id IN (:conversationIds)")
    suspend fun updateMultipleArchivedStatus(conversationIds: List<String>, archived: Boolean)

    @Query("UPDATE conversations SET pinned = :pinned, pinnedAt = :pinnedAt WHERE id = :conversationId")
    suspend fun updatePinnedStatus(conversationId: String, pinned: Boolean, pinnedAt: Long?)

    @Query("UPDATE conversations SET pinned = :pinned, pinnedAt = :pinnedAt WHERE id IN (:conversationIds)")
    suspend fun updateMultiplePinnedStatus(conversationIds: List<String>, pinned: Boolean, pinnedAt: Long?)

    @Query("UPDATE conversations SET muted = :muted WHERE id = :conversationId")
    suspend fun updateMutedStatus(conversationId: String, muted: Boolean)

    @Query("UPDATE conversations SET muted = :muted WHERE id IN (:conversationIds)")
    suspend fun updateMultipleMutedStatus(conversationIds: List<String>, muted: Boolean)

    // Groupes : mettre à jour la liste des admins
    @Query("UPDATE conversations SET adminIds = :adminIds WHERE id = :conversationId")
    suspend fun updateAdminIds(conversationId: String, adminIds: String)

    // Groupes : mettre à jour le lien d'invitation
    @Query("UPDATE conversations SET inviteLink = :inviteLink WHERE id = :conversationId")
    suspend fun updateInviteLink(conversationId: String, inviteLink: String?)

    // Éphémères : mettre à jour la durée des messages éphémères
    @Query("UPDATE conversations SET ephemeralDuration = :durationMs WHERE id = :conversationId")
    suspend fun updateEphemeralDuration(conversationId: String, durationMs: Long?)
}
