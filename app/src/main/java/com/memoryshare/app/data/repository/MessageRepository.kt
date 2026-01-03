package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.ConversationDao
import com.memoryshare.app.data.local.dao.MessageDao
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class MessageRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {

    fun getAllConversations(): Flow<List<Conversation>> = conversationDao.getAllConversations()

    fun getConversationById(conversationId: String): Flow<Conversation?> =
        conversationDao.getConversationById(conversationId)

    fun getMessagesByConversation(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId)

    suspend fun createConversation(
        participantIds: List<String>,
        name: String? = null,
        isGroup: Boolean = false
    ): Conversation {
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            name = name,
            isGroup = isGroup,
            participantIds = participantIds
        )
        conversationDao.insertConversation(conversation)
        return conversation
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
}
