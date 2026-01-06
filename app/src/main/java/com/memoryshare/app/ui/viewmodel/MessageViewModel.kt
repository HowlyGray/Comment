package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessageViewModel(
    private val repository: MessageRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages.asStateFlow()

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private var loadConversationJob: Job? = null
    private var loadMessagesJob: Job? = null

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            repository.getAllConversations().collect { conversations ->
                _conversations.value = conversations
            }
        }
    }

    fun loadConversation(conversationId: String) {
        // Annuler la collection précédente
        loadConversationJob?.cancel()
        loadConversationJob = viewModelScope.launch {
            repository.getConversationById(conversationId).collect { conversation ->
                _currentConversation.value = conversation
            }
        }
    }

    fun loadMessages(conversationId: String) {
        // Annuler la collection précédente
        loadMessagesJob?.cancel()
        loadMessagesJob = viewModelScope.launch {
            repository.getMessagesByConversation(conversationId).collect { messages ->
                _currentMessages.value = messages
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null
    ) {
        viewModelScope.launch {
            repository.sendMessage(conversationId, senderId, content, type, mediaUrl)
        }
    }

    suspend fun findOrCreateConversation(
        participantIds: List<String>,
        name: String? = null,
        isGroup: Boolean = false
    ): Conversation {
        return repository.findOrCreateConversation(participantIds, name, isGroup)
    }

    fun createConversation(participantIds: List<String>, name: String? = null, isGroup: Boolean = false) {
        viewModelScope.launch {
            repository.createConversation(participantIds, name, isGroup)
        }
    }

    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            repository.markConversationAsRead(conversationId)
        }
    }

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.deleteConversation(conversation)
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch {
            repository.editMessage(messageId, newContent)
        }
    }

    fun replyToMessage(
        conversationId: String,
        senderId: String,
        content: String,
        replyToMessageId: String
    ) {
        viewModelScope.launch {
            repository.replyToMessage(
                conversationId = conversationId,
                senderId = senderId,
                content = content,
                replyToId = replyToMessageId
            )
        }
    }

    fun addReaction(messageId: String, userId: String, emoji: String) {
        viewModelScope.launch {
            repository.addReaction(messageId, userId, emoji)
        }
    }

    // Starred messages methods
    fun getAllStarredMessages() = repository.getAllStarredMessages()

    fun getStarredMessagesByConversation(conversationId: String) =
        repository.getStarredMessagesByConversation(conversationId)

    fun toggleStarredStatus(messageId: String, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStarredStatus(messageId, isStarred)
        }
    }

    // Archived conversations methods
    fun getArchivedConversations() = repository.getArchivedConversations()

    fun getUnreadArchivedMessagesCount() = repository.getUnreadArchivedMessagesCount()

    fun archiveConversation(conversationId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveConversation(conversationId, archived)
        }
    }

    fun archiveMultipleConversations(conversationIds: List<String>, archived: Boolean) {
        viewModelScope.launch {
            repository.archiveMultipleConversations(conversationIds, archived)
        }
    }

    // Pin conversations methods
    fun pinConversation(conversationId: String, pinned: Boolean) {
        viewModelScope.launch {
            repository.pinConversation(conversationId, pinned)
        }
    }

    fun pinMultipleConversations(conversationIds: List<String>, pinned: Boolean) {
        viewModelScope.launch {
            repository.pinMultipleConversations(conversationIds, pinned)
        }
    }

    // Mute conversations methods
    fun muteConversation(conversationId: String, muted: Boolean) {
        viewModelScope.launch {
            repository.muteConversation(conversationId, muted)
        }
    }

    fun muteMultipleConversations(conversationIds: List<String>, muted: Boolean) {
        viewModelScope.launch {
            repository.muteMultipleConversations(conversationIds, muted)
        }
    }

    // Delete multiple conversations
    fun deleteMultipleConversations(conversationIds: List<String>) {
        viewModelScope.launch {
            repository.deleteMultipleConversations(conversationIds)
        }
    }

    // Delete multiple messages
    fun deleteMultipleMessages(messageIds: List<String>) {
        viewModelScope.launch {
            repository.deleteMultipleMessages(messageIds)
        }
    }

    // Star multiple messages
    fun starMultipleMessages(messageIds: List<String>, starred: Boolean) {
        viewModelScope.launch {
            repository.starMultipleMessages(messageIds, starred)
        }
    }

    // Forward messages
    fun forwardMessages(messageIds: List<String>, targetConversationId: String, senderId: String) {
        viewModelScope.launch {
            repository.forwardMessages(messageIds, targetConversationId, senderId)
        }
    }

    // Conversation by ID
    fun getConversationById(conversationId: String) = repository.getConversationById(conversationId)
}
