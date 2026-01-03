package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.data.repository.MessageRepository
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
        viewModelScope.launch {
            repository.getConversationById(conversationId).collect { conversation ->
                _currentConversation.value = conversation
            }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
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
}
