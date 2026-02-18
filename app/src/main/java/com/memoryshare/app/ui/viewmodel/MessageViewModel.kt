package com.memoryshare.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.MediaQuality
import com.memoryshare.app.data.model.MediaSourceType
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.data.repository.MessageRepository
import com.memoryshare.app.utils.FirebaseManager
import com.memoryshare.app.utils.MediaSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessageViewModel(
    private val repository: MessageRepository,
    private val mediaSyncManager: MediaSyncManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "MessageViewModel"
    }

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages.asStateFlow()

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private val _messagesToForward = MutableStateFlow<List<String>>(emptyList())
    val messagesToForward: StateFlow<List<String>> = _messagesToForward.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private var loadConversationJob: Job? = null
    private var loadMessagesJob: Job? = null
    
    private var messagesListener: ListenerRegistration? = null
    private var conversationsListener: ListenerRegistration? = null

    init {
        syncAndLoadConversations()
    }

    private fun syncAndLoadConversations() {
        val currentUserId = FirebaseManager.getCurrentUserId()
        
        // 1. Démarrer la synchronisation temps réel des conversations
        if (currentUserId != null) {
            conversationsListener?.remove()
            conversationsListener = repository.startConversationsRealtimeSync(currentUserId)
        }

        // 2. Observer la base locale pour les changements (Room)
        viewModelScope.launch {
            repository.getAllConversations().collect { conversations ->
                _conversations.value = conversations
            }
        }
        
        // Initial sync if needed (one shot)
        viewModelScope.launch {
            try {
                repository.syncConversationsFromFirebase()
            } catch (e: Exception) {
                Log.e(TAG, "Initial sync failed", e)
            }
        }
    }

    fun loadConversation(conversationId: String) {
        // Reset immédiat pour éviter de voir l'ancienne conversation
        _currentConversation.value = null
        
        loadConversationJob?.cancel()
        loadConversationJob = viewModelScope.launch {
            repository.getConversationById(conversationId).collect { conversation ->
                _currentConversation.value = conversation
            }
        }
    }

    fun loadMessages(conversationId: String) {
        // Reset immédiat des messages et passage en mode chargement
        _currentMessages.value = emptyList()
        _isLoadingMessages.value = true
        
        // Annuler les travaux et écouteurs précédents
        loadMessagesJob?.cancel()
        messagesListener?.remove()

        // 1. Démarrer la synchronisation temps réel pour cette conversation spécifique
        messagesListener = repository.startRealtimeSync(conversationId)

        // 2. Observer la base locale pour mettre à jour l'UI
        loadMessagesJob = viewModelScope.launch {
            repository.getMessagesByConversation(conversationId).collect { messages ->
                _currentMessages.value = messages
                _isLoadingMessages.value = false
            }
        }
        
        // Synchronisation ponctuelle avec Firebase
        viewModelScope.launch {
            repository.syncMessagesForConversation(conversationId)
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

    fun sendMessageWithMedia(
        conversationId: String,
        senderId: String,
        mediaUri: Uri,
        mediaType: MessageType,
        quality: MediaQuality = MediaQuality.SD,
        content: String = "",
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val finalUrl = if (mediaSyncManager != null) {
                    _uploadProgress.value = 0f
                    _uploadError.value = null

                    val cacheMediaType = when (mediaType) {
                        MessageType.IMAGE -> MediaType.IMAGE
                        MessageType.VIDEO -> MediaType.VIDEO
                        MessageType.AUDIO -> MediaType.AUDIO
                        else -> {
                            onError("Type de média non supporté")
                            return@launch
                        }
                    }

                    val prepareResult = mediaSyncManager.prepareMediaForUpload(
                        uri = mediaUri,
                        type = cacheMediaType,
                        quality = quality,
                        sourceType = MediaSourceType.MESSAGE,
                        sourceId = conversationId,
                        uploadedBy = senderId
                    )

                    if (prepareResult.isFailure) {
                        val error = prepareResult.exceptionOrNull()?.message ?: "Erreur de préparation du média"
                        _uploadError.value = error
                        onError(error)
                        return@launch
                    }

                    val mediaCache = prepareResult.getOrThrow()
                    _uploadProgress.value = 0.5f

                    val uploadResult = mediaSyncManager.uploadMedia(mediaCache)

                    if (uploadResult.isFailure) {
                        val error = uploadResult.exceptionOrNull()?.message ?: "Erreur d'upload"
                        _uploadError.value = error
                        onError(error)
                        return@launch
                    }

                    _uploadProgress.value = 1f
                    val firebaseUrl = uploadResult.getOrThrow()
                    _uploadProgress.value = null
                    firebaseUrl
                } else {
                    onError("Service de gestion des médias non disponible")
                    return@launch
                }

                repository.sendMessage(conversationId, senderId, content, mediaType, finalUrl)
                onSuccess(finalUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message with media", e)
                _uploadError.value = e.message
                onError(e.message ?: "Erreur d'envoi du message")
            }
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

    // ACK : marquer les messages comme lus (READ) et délivrés (DELIVERED)
    fun markMessagesRead(conversationId: String, currentUserId: String) {
        viewModelScope.launch {
            repository.markConversationMessagesRead(conversationId, currentUserId)
        }
    }

    fun markMessagesDelivered(conversationId: String, currentUserId: String) {
        viewModelScope.launch {
            repository.markConversationMessagesDelivered(conversationId, currentUserId)
        }
    }

    // Admin groupes
    fun promoteToAdmin(conversationId: String, userId: String) {
        viewModelScope.launch { repository.promoteToAdmin(conversationId, userId) }
    }

    fun demoteAdmin(conversationId: String, userId: String) {
        viewModelScope.launch { repository.demoteAdmin(conversationId, userId) }
    }

    suspend fun generateInviteLink(conversationId: String): String =
        repository.generateInviteLink(conversationId)

    fun revokeInviteLink(conversationId: String) {
        viewModelScope.launch { repository.revokeInviteLink(conversationId) }
    }

    // Messages éphémères
    fun setEphemeralDuration(conversationId: String, durationMs: Long?) {
        viewModelScope.launch { repository.setEphemeralDuration(conversationId, durationMs) }
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

    fun getReactionsForMessage(messageId: String) = repository.getReactionsForMessage(messageId)

    fun getAllStarredMessages() = repository.getAllStarredMessages()

    fun getStarredMessagesByConversation(conversationId: String) =
        repository.getStarredMessagesByConversation(conversationId)

    fun toggleStarredStatus(messageId: String, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStarredStatus(messageId, isStarred)
        }
    }

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

    fun deleteMultipleConversations(conversationIds: List<String>) {
        viewModelScope.launch {
            repository.deleteMultipleConversations(conversationIds)
        }
    }

    fun deleteMultipleMessages(messageIds: List<String>) {
        viewModelScope.launch {
            repository.deleteMultipleMessages(messageIds)
        }
    }

    fun starMultipleMessages(messageIds: List<String>, starred: Boolean) {
        viewModelScope.launch {
            repository.starMultipleMessages(messageIds, starred)
        }
    }

    fun forwardMessages(messageIds: List<String>, targetConversationId: String, senderId: String) {
        viewModelScope.launch {
            repository.forwardMessages(messageIds, targetConversationId, senderId)
        }
    }

    fun setMessagesToForward(messageIds: List<String>) {
        _messagesToForward.value = messageIds
    }

    fun clearMessagesToForward() {
        _messagesToForward.value = emptyList()
    }

    fun getConversationById(conversationId: String) = repository.getConversationById(conversationId)

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        conversationsListener?.remove()
    }
}
