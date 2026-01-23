package com.memoryshare.app.data.repository

import android.util.Log
import com.memoryshare.app.data.local.dao.ConversationDao
import com.memoryshare.app.data.local.dao.MessageDao
import com.memoryshare.app.data.local.dao.MessageReactionDao
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageReaction
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.utils.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class MessageRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val messageReactionDao: MessageReactionDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore = FirebaseManager.firestore

    companion object {
        private const val TAG = "MessageRepository"
    }

    fun getAllConversations(): Flow<List<Conversation>> = conversationDao.getAllConversations()

    fun getConversationById(conversationId: String): Flow<Conversation?> =
        conversationDao.getConversationById(conversationId)

    fun getMessagesByConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId)

    suspend fun findOrCreateConversation(
        participantIds: List<String>,
        name: String? = null,
        isGroup: Boolean = false
    ): Conversation {
        // Vérifier si une conversation existe déjà avec les mêmes participants
        if (!isGroup && participantIds.size == 2) {
            val existingConversations = conversationDao.getAllConversationsSync()
            val existingConversation = existingConversations.find { conv ->
                !conv.isGroup &&
                conv.participantIds.size == 2 &&
                conv.participantIds.containsAll(participantIds)
            }
            if (existingConversation != null) {
                return existingConversation
            }
        }

        // Sinon, créer une nouvelle conversation
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            name = name,
            isGroup = isGroup,
            participantIds = participantIds
        )
        // Sauvegarder localement
        conversationDao.insertConversation(conversation)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.CONVERSATIONS,
                documentId = conversation.id,
                data = conversation
            ).onSuccess {
                Log.d(TAG, "Conversation synced to Firebase: ${conversation.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync conversation to Firebase: ${conversation.id}", e)
            }
        }

        return conversation
    }

    suspend fun createConversation(
        participantIds: List<String>,
        name: String? = null,
        isGroup: Boolean = false
    ): Conversation {
        return findOrCreateConversation(participantIds, name, isGroup)
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null
    ): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            type = type,
            mediaUrl = mediaUrl
        )
        // Sauvegarder localement
        messageDao.insertMessage(message)

        // Mettre à jour la conversation avec le dernier message localement
        val conversation = conversationDao.getConversationById(conversationId).firstOrNull()
        conversation?.let {
            val updatedConv = it.copy(
                lastMessageText = content,
                lastMessageTime = message.timestamp
            )
            conversationDao.updateConversation(updatedConv)

            // Synchroniser avec Firebase
            scope.launch {
                // Sauvegarder le message
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.MESSAGES,
                    documentId = message.id,
                    data = message
                ).onSuccess {
                    Log.d(TAG, "Message synced to Firebase: ${message.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to sync message to Firebase: ${message.id}", e)
                }

                // Mettre à jour la conversation
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.CONVERSATIONS,
                    documentId = updatedConv.id,
                    data = updatedConv
                ).onSuccess {
                    Log.d(TAG, "Conversation updated in Firebase: ${updatedConv.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to update conversation in Firebase: ${updatedConv.id}", e)
                }
            }
        }

        return message
    }

    suspend fun markConversationAsRead(conversationId: String) {
        messageDao.markConversationAsRead(conversationId)
    }

    suspend fun deleteConversation(conversation: Conversation) {
        // Supprimer localement
        messageDao.deleteMessagesByConversation(conversation.id)
        conversationDao.deleteConversation(conversation)

        // Supprimer de Firebase
        scope.launch {
            FirebaseManager.deleteDocument(
                collection = FirebaseManager.Collections.CONVERSATIONS,
                documentId = conversation.id
            ).onSuccess {
                Log.d(TAG, "Conversation deleted from Firebase: ${conversation.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete conversation from Firebase: ${conversation.id}", e)
            }
        }
    }

    suspend fun deleteMessage(message: Message) {
        // Supprimer localement
        messageDao.deleteMessage(message)

        // Supprimer de Firebase
        scope.launch {
            FirebaseManager.deleteDocument(
                collection = FirebaseManager.Collections.MESSAGES,
                documentId = message.id
            ).onSuccess {
                Log.d(TAG, "Message deleted from Firebase: ${message.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete message from Firebase: ${message.id}", e)
            }
        }
    }

    // Edit message
    suspend fun editMessage(messageId: String, newContent: String) {
        val message = messageDao.getMessageByIdSync(messageId)
        message?.let {
            val updatedMessage = it.copy(
                content = newContent,
                editedAt = System.currentTimeMillis()
            )
            // Mettre à jour localement
            messageDao.updateMessage(updatedMessage)

            // Synchroniser avec Firebase
            scope.launch {
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.MESSAGES,
                    documentId = updatedMessage.id,
                    data = updatedMessage
                ).onSuccess {
                    Log.d(TAG, "Message edit synced to Firebase: ${updatedMessage.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to sync message edit to Firebase: ${updatedMessage.id}", e)
                }
            }
        }
    }

    // Reply to message
    suspend fun replyToMessage(
        conversationId: String,
        senderId: String,
        content: String,
        replyToId: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null
    ): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            type = type,
            mediaUrl = mediaUrl,
            replyToId = replyToId
        )
        messageDao.insertMessage(message)

        // Mettre à jour la conversation
        val conversation = conversationDao.getConversationById(conversationId).firstOrNull()
        conversation?.let {
            conversationDao.updateConversation(
                it.copy(
                    lastMessageText = content,
                    lastMessageTime = message.timestamp
                )
            )
        }

        return message
    }

    fun getMessageByIdFlow(messageId: String): Flow<Message?> = messageDao.getMessageById(messageId)

    suspend fun searchInConversation(conversationId: String, query: String): Flow<List<Message>> =
        messageDao.searchInConversation(conversationId, query)

    // Message reactions
    fun getReactionsForMessage(messageId: String): Flow<List<MessageReaction>> =
        messageReactionDao.getReactionsForMessage(messageId)

    suspend fun addReaction(messageId: String, userId: String, emoji: String) {
        // Supprimer l'ancienne réaction de l'utilisateur si elle existe
        messageReactionDao.deleteUserReaction(messageId, userId)

        // Ajouter la nouvelle réaction
        val reaction = MessageReaction(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            userId = userId,
            emoji = emoji
        )
        messageReactionDao.insertReaction(reaction)
    }

    suspend fun removeReaction(messageId: String, userId: String) {
        messageReactionDao.deleteUserReaction(messageId, userId)
    }

    // Starred messages
    fun getAllStarredMessages(): Flow<List<Message>> = messageDao.getAllStarredMessages()

    fun getStarredMessagesByConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getStarredMessagesByConversation(conversationId)

    suspend fun toggleStarredStatus(messageId: String, isStarred: Boolean) {
        messageDao.updateStarredStatus(messageId, isStarred)
    }

    // Archived conversations
    fun getArchivedConversations(): Flow<List<Conversation>> =
        conversationDao.getArchivedConversations()

    fun getUnreadArchivedMessagesCount(): Flow<Int> =
        messageDao.getUnreadArchivedMessagesCount()

    suspend fun archiveConversation(conversationId: String, archived: Boolean) {
        conversationDao.updateArchivedStatus(conversationId, archived)
        // Auto-mute when archiving
        if (archived) {
            conversationDao.updateMutedStatus(conversationId, true)
        }
    }

    suspend fun archiveMultipleConversations(conversationIds: List<String>, archived: Boolean) {
        conversationDao.updateMultipleArchivedStatus(conversationIds, archived)
        // Auto-mute when archiving
        if (archived) {
            conversationDao.updateMultipleMutedStatus(conversationIds, true)
        }
    }

    // Pin conversations
    suspend fun pinConversation(conversationId: String, pinned: Boolean) {
        val pinnedAt = if (pinned) System.currentTimeMillis() else null
        conversationDao.updatePinnedStatus(conversationId, pinned, pinnedAt)
    }

    suspend fun pinMultipleConversations(conversationIds: List<String>, pinned: Boolean) {
        val pinnedAt = if (pinned) System.currentTimeMillis() else null
        conversationDao.updateMultiplePinnedStatus(conversationIds, pinned, pinnedAt)
    }

    // Mute conversations
    suspend fun muteConversation(conversationId: String, muted: Boolean) {
        conversationDao.updateMutedStatus(conversationId, muted)
    }

    suspend fun muteMultipleConversations(conversationIds: List<String>, muted: Boolean) {
        conversationDao.updateMultipleMutedStatus(conversationIds, muted)
    }

    // Delete multiple conversations
    suspend fun deleteMultipleConversations(conversationIds: List<String>) {
        conversationIds.forEach { conversationId ->
            messageDao.deleteMessagesByConversation(conversationId)
        }
        conversationDao.deleteConversations(conversationIds)
    }

    // Delete multiple messages
    suspend fun deleteMultipleMessages(messageIds: List<String>) {
        messageDao.deleteMessages(messageIds)
    }

    // Star multiple messages
    suspend fun starMultipleMessages(messageIds: List<String>, starred: Boolean) {
        messageDao.updateMultipleStarredStatus(messageIds, starred)
    }

    // Forward messages to another conversation
    suspend fun forwardMessages(
        messageIds: List<String>,
        targetConversationId: String,
        senderId: String
    ) {
        messageIds.forEach { messageId ->
            val originalMessage = messageDao.getMessageByIdSync(messageId)
            originalMessage?.let { message ->
                val forwardedMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = targetConversationId,
                    senderId = senderId,
                    content = message.content,
                    type = message.type,
                    mediaUrl = message.mediaUrl,
                    timestamp = System.currentTimeMillis()
                )
                messageDao.insertMessage(forwardedMessage)

                // Update target conversation
                val conversation = conversationDao.getConversationById(targetConversationId).firstOrNull()
                conversation?.let { conv ->
                    conversationDao.updateConversation(
                        conv.copy(
                            lastMessageText = forwardedMessage.content,
                            lastMessageTime = forwardedMessage.timestamp
                        )
                    )
                }
            }
        }
    }

    /**
     * Synchronise les conversations depuis Firebase vers la base de données locale
     */
    suspend fun syncConversationsFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.CONVERSATIONS,
            clazz = Conversation::class.java
        ).onSuccess { conversations ->
            conversations.forEach { conversation ->
                conversationDao.insertConversation(conversation)
            }
            Log.d(TAG, "Synced ${conversations.size} conversations from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync conversations from Firebase", e)
        }
    }

    /**
     * Synchronise les messages depuis Firebase vers la base de données locale
     */
    suspend fun syncMessagesFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.MESSAGES,
            clazz = Message::class.java
        ).onSuccess { messages ->
            messages.forEach { message ->
                messageDao.insertMessage(message)
            }
            Log.d(TAG, "Synced ${messages.size} messages from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync messages from Firebase", e)
        }
    }
}
