package com.memoryshare.app.services.realtime

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.memoryshare.app.data.local.dao.ConversationDao
import com.memoryshare.app.data.local.dao.MessageDao
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.utils.FirestoreMappers.toConversation
import com.memoryshare.app.utils.FirestoreMappers.toMessage
import com.memoryshare.app.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Realtime Sync Manager
 * Synchronizes Firestore data with local Room database in real-time
 */
class RealtimeSyncManager(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val context: android.content.Context? = null
) {
    companion object {
        private const val TAG = "RealtimeSyncManager"
        private const val CONVERSATIONS = "conversations"
        private const val MESSAGES = "messages"
        // Max concurrent message listeners to limit resource usage
        private const val MAX_MESSAGE_LISTENERS = 20
        // Max messages to sync per conversation
        private const val MESSAGE_SYNC_LIMIT = 100
    }

    private val db: FirebaseFirestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentUserId: String? = null
    private var conversationsListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    /**
     * Check if the sync manager has been initialized
     */
    fun isInitialized(): Boolean = currentUserId != null

    /**
     * Initialize sync manager with user ID
     */
    fun initialize(userId: String) {
        currentUserId = userId
        startConversationsSync()
    }

    /**
     * Start syncing conversations
     */
    private fun startConversationsSync() {
        val userId = currentUserId ?: return
        Log.d(TAG, "Starting conversations sync for user: $userId")

        conversationsListener?.remove()
        conversationsListener = db.collection(CONVERSATIONS)
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Conversations sync error", error)
                    return@addSnapshotListener
                }

                scope.launch {
                    val changes = snapshot?.documentChanges ?: emptyList()
                    for (change in changes) {
                        val conversation = change.document.toConversation()
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                conversation?.let {
                                    conversationDao.insertConversation(it)
                                    // Start listening to messages for this conversation
                                    startMessagesSync(it.id)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                conversation?.let {
                                    conversationDao.deleteConversation(it)
                                    stopMessagesSync(it.id)
                                }
                            }
                        }
                    }
                }
            }
    }

    /**
     * Start syncing messages for a conversation.
     * Uses the subcollection path: conversations/{conversationId}/messages
     * to match FirestoreManager's storage format.
     * Limits concurrent listeners to MAX_MESSAGE_LISTENERS.
     */
    fun startMessagesSync(conversationId: String) {
        if (messageListeners.containsKey(conversationId)) {
            return // Already listening
        }

        // Limit concurrent listeners to avoid excessive resource usage
        if (messageListeners.size >= MAX_MESSAGE_LISTENERS) {
            // Remove the oldest listener
            val oldest = messageListeners.keys.firstOrNull()
            oldest?.let { stopMessagesSync(it) }
        }

        Log.d(TAG, "Starting messages sync for conversation: $conversationId")

        val listener = db.collection(CONVERSATIONS)
            .document(conversationId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(MESSAGE_SYNC_LIMIT.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Messages sync error for $conversationId", error)
                    return@addSnapshotListener
                }

                scope.launch {
                    val changes = snapshot?.documentChanges ?: emptyList()
                    for (change in changes) {
                        val message = change.document.toMessage()
                        when (change.type) {
                            DocumentChange.Type.ADDED -> {
                                message?.let { msg ->
                                    messageDao.insertMessage(msg)
                                    if (msg.senderId != currentUserId && context != null) {
                                        val preview = when (msg.type) {
                                            MessageType.IMAGE -> "Photo"
                                            MessageType.VIDEO -> "Video"
                                            MessageType.AUDIO -> "Audio"
                                            MessageType.FILE -> "File"
                                            else -> msg.content
                                        }
                                        NotificationHelper.notifyNewMessage(
                                            context = context,
                                            senderName = msg.senderId,
                                            messagePreview = preview,
                                            conversationId = msg.conversationId
                                        )
                                    }
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                message?.let { messageDao.insertMessage(it) }
                            }
                            DocumentChange.Type.REMOVED -> {
                                message?.let { messageDao.deleteMessage(it) }
                            }
                        }
                    }
                }
            }

        messageListeners[conversationId] = listener
    }

    /**
     * Stop syncing messages for a conversation
     */
    fun stopMessagesSync(conversationId: String) {
        messageListeners[conversationId]?.remove()
        messageListeners.remove(conversationId)
    }

    /**
     * Observe new messages in real-time (returns Flow)
     */
    fun observeNewMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection(CONVERSATIONS)
            .document(conversationId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { it.toMessage() } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe conversation updates
     */
    fun observeConversation(conversationId: String): Flow<Conversation?> = callbackFlow {
        val listener = db.collection(CONVERSATIONS)
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.toConversation())
            }

        awaitClose { listener.remove() }
    }

    /**
     * Manual sync - fetch all data once
     */
    suspend fun performFullSync() {
        val userId = currentUserId ?: return

        try {
            // Sync conversations
            val conversationsSnapshot = db.collection(CONVERSATIONS)
                .whereArrayContains("participantIds", userId)
                .get()
                .await()

            for (doc in conversationsSnapshot.documents) {
                doc.toConversation()?.let { conversation ->
                    conversationDao.insertConversation(conversation)

                    // Sync latest messages for each conversation
                    val messagesSnapshot = db.collection(CONVERSATIONS)
                        .document(conversation.id)
                        .collection(MESSAGES)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(MESSAGE_SYNC_LIMIT.toLong())
                        .get()
                        .await()

                    val messages = messagesSnapshot.documents.mapNotNull { it.toMessage() }
                    for (message in messages) {
                        messageDao.insertMessage(message)
                    }
                }
            }

            Log.d(TAG, "Full sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
        }
    }

    /**
     * Sync single conversation
     */
    suspend fun syncConversation(conversationId: String) {
        try {
            val doc = db.collection(CONVERSATIONS).document(conversationId).get().await()
            doc.toConversation()?.let { conversationDao.insertConversation(it) }

            val messagesSnapshot = db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MESSAGE_SYNC_LIMIT.toLong())
                .get()
                .await()

            val messages = messagesSnapshot.documents.mapNotNull { it.toMessage() }
            for (message in messages) {
                messageDao.insertMessage(message)
            }

            Log.d(TAG, "Conversation $conversationId synced")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync conversation $conversationId", e)
        }
    }

    /**
     * Stop all sync operations
     */
    fun stopAllSync() {
        conversationsListener?.remove()
        conversationsListener = null

        for (listener in messageListeners.values) {
            listener.remove()
        }
        messageListeners.clear()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopAllSync()
        currentUserId = null
        scope.cancel()
    }

}
