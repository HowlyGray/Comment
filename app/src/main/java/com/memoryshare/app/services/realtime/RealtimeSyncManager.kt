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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val conversationDao: ConversationDao
) {
    companion object {
        private const val TAG = "RealtimeSyncManager"
        private const val CONVERSATIONS = "conversations"
        private const val MESSAGES = "messages"
    }

    private val db: FirebaseFirestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentUserId: String? = null
    private var conversationsListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

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

        conversationsListener?.remove()
        conversationsListener = db.collection(CONVERSATIONS)
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
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
     * Start syncing messages for a conversation
     */
    fun startMessagesSync(conversationId: String) {
        if (messageListeners.containsKey(conversationId)) {
            return // Already listening
        }

        val listener = db.collection(CONVERSATIONS)
            .document(conversationId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
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
                            DocumentChange.Type.ADDED,
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

                    // Sync messages for each conversation
                    val messagesSnapshot = db.collection(CONVERSATIONS)
                        .document(conversation.id)
                        .collection(MESSAGES)
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
    }

    // Extension functions for document conversion
    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toConversation(): Conversation? {
        return try {
            Conversation(
                id = getString("id") ?: id,
                name = getString("name"),
                isGroup = getBoolean("isGroup") ?: false,
                participantIds = get("participantIds") as? List<String> ?: emptyList(),
                lastMessageText = getString("lastMessageText"),
                lastMessageTime = getLong("lastMessageTime"),
                imageUrl = getString("imageUrl"),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                archived = getBoolean("archived") ?: false,
                pinned = getBoolean("pinned") ?: false,
                pinnedAt = getLong("pinnedAt"),
                muted = getBoolean("muted") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert document to Conversation", e)
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toMessage(): Message? {
        return try {
            Message(
                id = getString("id") ?: id,
                conversationId = getString("conversationId") ?: "",
                senderId = getString("senderId") ?: "",
                content = getString("content") ?: "",
                type = MessageType.valueOf(getString("type") ?: "TEXT"),
                timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
                isRead = getBoolean("isRead") ?: false,
                isStarred = getBoolean("isStarred") ?: false,
                mediaUrl = getString("mediaUrl"),
                mediaThumbnailUrl = getString("mediaThumbnailUrl"),
                mediaDuration = getLong("mediaDuration"),
                replyToId = getString("replyToId"),
                editedAt = getLong("editedAt")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert document to Message", e)
            null
        }
    }
}
