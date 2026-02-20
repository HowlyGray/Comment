package com.memoryshare.app.services.realtime

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.memoryshare.app.data.model.PrivacySettings
import com.memoryshare.app.data.model.PrivacyVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Presence Manager
 * Handles online/offline status and typing indicators
 */
class PresenceManager : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "PresenceManager"
        private const val PRESENCE_COLLECTION = "presence"
        private const val TYPING_COLLECTION = "typing"
        private const val FIELD_IS_ONLINE = "isOnline"
        private const val FIELD_LAST_SEEN = "lastSeen"
        private const val FIELD_IS_TYPING = "isTyping"
        private const val FIELD_CONVERSATION_ID = "conversationId"
        private const val TYPING_TIMEOUT_MS = 5000L
    }

    private val db: FirebaseFirestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentUserId: String? = null
    private var currentPrivacySettings: PrivacySettings = PrivacySettings()
    private var presenceListener: ListenerRegistration? = null
    private val typingListeners = mutableMapOf<String, ListenerRegistration>()

    // Current user's online status
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Users currently being observed
    private val _observedUsersPresence = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val observedUsersPresence: StateFlow<Map<String, UserPresence>> = _observedUsersPresence.asStateFlow()

    /**
     * Initialize presence manager with user ID
     */
    fun initialize(userId: String) {
        currentUserId = userId
        goOnline()
    }

    /**
     * Met à jour les paramètres de confidentialité locaux et les persiste dans Firestore
     */
    fun updatePrivacySettings(settings: PrivacySettings) {
        currentPrivacySettings = settings
        val userId = currentUserId ?: return
        scope.launch {
            try {
                db.collection(PRESENCE_COLLECTION).document(userId).set(
                    mapOf("privacySettings" to settings.toMap()),
                    SetOptions.merge()
                ).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save privacy settings", e)
            }
        }
    }

    /**
     * Set user as online — respecte le paramètre showOnlineStatus
     */
    fun goOnline() {
        val userId = currentUserId ?: return
        // Si l'utilisateur a choisi de masquer son statut en ligne, on n'envoie pas isOnline=true
        val publishOnline = currentPrivacySettings.showOnlineStatus != PrivacyVisibility.NOBODY

        scope.launch {
            try {
                val data = mutableMapOf<String, Any>(
                    FIELD_LAST_SEEN to System.currentTimeMillis()
                )
                if (publishOnline) data[FIELD_IS_ONLINE] = true

                db.collection(PRESENCE_COLLECTION).document(userId).set(
                    data, SetOptions.merge()
                ).await()

                _isOnline.value = true
                Log.d(TAG, "User $userId is now online (publish=$publishOnline)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set online status", e)
            }
        }
    }

    /**
     * Set user as offline — respecte le paramètre showLastSeen
     */
    fun goOffline() {
        val userId = currentUserId ?: return
        val publishLastSeen = currentPrivacySettings.showLastSeen != PrivacyVisibility.NOBODY

        scope.launch {
            try {
                val data = mutableMapOf<String, Any>(
                    FIELD_IS_ONLINE to false
                )
                if (publishLastSeen) data[FIELD_LAST_SEEN] = System.currentTimeMillis()

                db.collection(PRESENCE_COLLECTION).document(userId).set(
                    data, SetOptions.merge()
                ).await()

                _isOnline.value = false
                Log.d(TAG, "User $userId is now offline (publishLastSeen=$publishLastSeen)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set offline status", e)
            }
        }
    }

    /**
     * Update last seen timestamp
     */
    fun updateLastSeen() {
        val userId = currentUserId ?: return

        scope.launch {
            try {
                db.collection(PRESENCE_COLLECTION).document(userId).update(
                    FIELD_LAST_SEEN, System.currentTimeMillis()
                ).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update last seen", e)
            }
        }
    }

    /**
     * Observe user presence — applique les règles de confidentialité de la cible
     */
    fun observeUserPresence(userId: String): Flow<UserPresence> = callbackFlow {
        val listener = db.collection(PRESENCE_COLLECTION).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing presence for $userId", error)
                    return@addSnapshotListener
                }

                val presence = if (snapshot?.exists() == true) {
                    // Lire les paramètres de confidentialité de la cible
                    @Suppress("UNCHECKED_CAST")
                    val privacyMap = snapshot.get("privacySettings") as? Map<String, Any?> ?: emptyMap()
                    val targetPrivacy = PrivacySettings.fromMap(privacyMap)

                    val canSeeOnline = targetPrivacy.showOnlineStatus != PrivacyVisibility.NOBODY
                    val canSeeLastSeen = targetPrivacy.showLastSeen != PrivacyVisibility.NOBODY

                    UserPresence(
                        userId = userId,
                        isOnline = if (canSeeOnline) snapshot.getBoolean(FIELD_IS_ONLINE) ?: false else false,
                        lastSeen = if (canSeeLastSeen) snapshot.getLong(FIELD_LAST_SEEN) ?: 0L else 0L
                    )
                } else {
                    UserPresence(userId = userId, isOnline = false, lastSeen = 0L)
                }

                trySend(presence)

                val current = _observedUsersPresence.value.toMutableMap()
                current[userId] = presence
                _observedUsersPresence.value = current
            }

        awaitClose {
            listener.remove()
            val current = _observedUsersPresence.value.toMutableMap()
            current.remove(userId)
            _observedUsersPresence.value = current
        }
    }

    /**
     * Get user presence once
     */
    suspend fun getUserPresence(userId: String): UserPresence? {
        return try {
            val doc = db.collection(PRESENCE_COLLECTION).document(userId).get().await()
            if (doc.exists()) {
                UserPresence(
                    userId = userId,
                    isOnline = doc.getBoolean(FIELD_IS_ONLINE) ?: false,
                    lastSeen = doc.getLong(FIELD_LAST_SEEN) ?: 0L
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get presence for $userId", e)
            null
        }
    }

    /**
     * Set typing status
     */
    fun setTyping(conversationId: String, isTyping: Boolean) {
        val userId = currentUserId ?: return

        scope.launch {
            try {
                val typingDocId = "${conversationId}_$userId"
                if (isTyping) {
                    db.collection(TYPING_COLLECTION).document(typingDocId).set(
                        mapOf(
                            FIELD_IS_TYPING to true,
                            FIELD_CONVERSATION_ID to conversationId,
                            "userId" to userId,
                            "timestamp" to System.currentTimeMillis()
                        )
                    ).await()
                } else {
                    db.collection(TYPING_COLLECTION).document(typingDocId).delete().await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set typing status", e)
            }
        }
    }

    /**
     * Observe typing users in conversation
     */
    fun observeTypingUsers(conversationId: String): Flow<List<String>> = callbackFlow {
        val listener = db.collection(TYPING_COLLECTION)
            .whereEqualTo(FIELD_CONVERSATION_ID, conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing typing in $conversationId", error)
                    return@addSnapshotListener
                }

                val currentTime = System.currentTimeMillis()
                val typingUsers = snapshot?.documents
                    ?.filter { doc ->
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        // Only show typing if within timeout
                        (currentTime - timestamp) < TYPING_TIMEOUT_MS
                    }
                    ?.mapNotNull { it.getString("userId") }
                    ?.filter { it != currentUserId } // Don't show current user
                    ?: emptyList()

                trySend(typingUsers)
            }

        typingListeners[conversationId] = listener

        awaitClose {
            listener.remove()
            typingListeners.remove(conversationId)
        }
    }

    /**
     * Observe multiple users presence
     */
    fun observeUsersPresence(userIds: List<String>): Flow<Map<String, UserPresence>> = callbackFlow {
        if (userIds.isEmpty()) {
            trySend(emptyMap())
            awaitClose { }
            return@callbackFlow
        }

        // Firestore has a limit of 30 for whereIn queries
        val chunks = userIds.chunked(30)
        val listeners = mutableListOf<ListenerRegistration>()
        val presenceMap = mutableMapOf<String, UserPresence>()

        chunks.forEach { chunk ->
            val listener = db.collection(PRESENCE_COLLECTION)
                .whereIn("__name__", chunk.map { db.collection(PRESENCE_COLLECTION).document(it) })
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing multiple presence", error)
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.forEach { doc ->
                        presenceMap[doc.id] = UserPresence(
                            userId = doc.id,
                            isOnline = doc.getBoolean(FIELD_IS_ONLINE) ?: false,
                            lastSeen = doc.getLong(FIELD_LAST_SEEN) ?: 0L
                        )
                    }

                    trySend(presenceMap.toMap())
                }
            listeners.add(listener)
        }

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    /**
     * Format last seen time for display
     */
    fun formatLastSeen(lastSeen: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - lastSeen

        return when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000} minutes ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                sdf.format(java.util.Date(lastSeen))
            }
        }
    }

    // Lifecycle callbacks
    override fun onStart(owner: LifecycleOwner) {
        goOnline()
    }

    override fun onStop(owner: LifecycleOwner) {
        goOffline()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cleanup()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        goOffline()
        presenceListener?.remove()
        typingListeners.values.forEach { it.remove() }
        typingListeners.clear()
        scope.cancel()
    }

    /**
     * User presence data class
     */
    data class UserPresence(
        val userId: String,
        val isOnline: Boolean,
        val lastSeen: Long
    )
}
