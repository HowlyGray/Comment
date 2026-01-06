package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.ConversationDao
import com.memoryshare.app.data.local.dao.MessageDao
import com.memoryshare.app.data.local.dao.MessageReactionDao
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageReaction
import com.memoryshare.app.data.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class MessageRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val messageReactionDao: MessageReactionDao
) {

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
        conversationDao.insertConversation(conversation)
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
        messageDao.insertMessage(message)

        // Mettre à jour la conversation avec le dernier message
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

    suspend fun markConversationAsRead(conversationId: String) {
        messageDao.markConversationAsRead(conversationId)
    }

    suspend fun deleteConversation(conversation: Conversation) {
        messageDao.deleteMessagesByConversation(conversation.id)
        conversationDao.deleteConversation(conversation)
    }

    suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(message)
    }

    // Edit message
    suspend fun editMessage(messageId: String, newContent: String) {
        val message = messageDao.getMessageByIdSync(messageId)
        message?.let {
            val updatedMessage = it.copy(
                content = newContent,
                editedAt = System.currentTimeMillis()
            )
            messageDao.updateMessage(updatedMessage)
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

    suspend fun archiveConversation(conversationId: String, archived: Boolean) {
        conversationDao.updateArchivedStatus(conversationId, archived)
    }

    suspend fun archiveMultipleConversations(conversationIds: List<String>, archived: Boolean) {
        conversationDao.updateMultipleArchivedStatus(conversationIds, archived)
    }

    // Pin conversations
    suspend fun pinConversation(conversationId: String, pinned: Boolean) {
        conversationDao.updatePinnedStatus(conversationId, pinned)
    }

    suspend fun pinMultipleConversations(conversationIds: List<String>, pinned: Boolean) {
        conversationDao.updateMultiplePinnedStatus(conversationIds, pinned)
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
}
