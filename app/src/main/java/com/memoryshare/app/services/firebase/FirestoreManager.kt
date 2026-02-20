package com.memoryshare.app.services.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.memoryshare.app.data.model.*
import com.memoryshare.app.utils.FirestoreMappers.toConversation
import com.memoryshare.app.utils.FirestoreMappers.toMessage
import com.memoryshare.app.utils.FirestoreMappers.toPost
import com.memoryshare.app.utils.FirestoreMappers.toSharedSpace
import com.memoryshare.app.utils.FirestoreMappers.toStory
import com.memoryshare.app.utils.FirestoreMappers.toUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firestore Manager
 * Handles all Firestore database operations with real-time sync capabilities
 */
class FirestoreManager {

    private val db: FirebaseFirestore = Firebase.firestore

    // Collection names
    companion object {
        const val USERS = "users"
        const val CONVERSATIONS = "conversations"
        const val MESSAGES = "messages"
        const val POSTS = "posts"
        const val COMMENTS = "comments"
        const val STORIES = "stories"
        const val REELS = "reels"
        const val SHARED_SPACES = "shared_spaces"
        const val MEDIA = "media"
        const val FOLLOWS = "follows"
        const val REACTIONS = "reactions"
        const val CALLS = "calls"
        const val PRESENCE = "presence"
        const val FCM_TOKENS = "fcm_tokens"
        // Firestore batch operation limit
        private const val BATCH_LIMIT = 500
    }

    // ==================== USER OPERATIONS ====================

    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            db.collection(USERS).document(user.id).set(user.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val doc = db.collection(USERS).document(userId).get().await()
            Result.success(doc.toUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = db.collection(USERS).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUser())
            }
        awaitClose { listener.remove() }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val results = db.collection(USERS)
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            Result.success(results.documents.mapNotNull { it.toUser() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== CONVERSATION OPERATIONS ====================

    suspend fun createConversation(conversation: Conversation): Result<String> {
        return try {
            val docRef = db.collection(CONVERSATIONS).document(conversation.id)
            docRef.set(conversation.toMap()).await()
            Result.success(conversation.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = db.collection(CONVERSATIONS)
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull { it.toConversation() } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateConversationLastMessage(
        conversationId: String,
        message: String,
        timestamp: Long
    ): Result<Unit> {
        return try {
            db.collection(CONVERSATIONS).document(conversationId).update(
                mapOf(
                    "lastMessageText" to message,
                    "lastMessageTime" to timestamp
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== MESSAGE OPERATIONS ====================

    suspend fun sendMessage(conversationId: String, message: Message): Result<String> {
        return try {
            val docRef = db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .document(message.id)
            docRef.set(message.toMap()).await()

            updateConversationLastMessage(
                conversationId,
                if (message.type == MessageType.TEXT) message.content else "[${message.type.name}]",
                message.timestamp
            )

            Result.success(message.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMessages(conversationId: String, limit: Int = 100): Flow<List<Message>> = callbackFlow {
        val listener = db.collection(CONVERSATIONS)
            .document(conversationId)
            .collection(MESSAGES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toMessage() }?.reversed() ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateMessage(
        conversationId: String,
        messageId: String,
        newContent: String
    ): Result<Unit> {
        return try {
            db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .document(messageId)
                .update(
                    mapOf(
                        "content" to newContent,
                        "editedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReaction(
        conversationId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            val reactionId = "${messageId}_${userId}"
            db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .document(messageId)
                .collection(REACTIONS)
                .document(reactionId)
                .set(
                    mapOf(
                        "userId" to userId,
                        "emoji" to emoji,
                        "timestamp" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== POST OPERATIONS ====================

    suspend fun createPost(post: Post): Result<String> {
        return try {
            db.collection(POSTS).document(post.id).set(post.toMap()).await()
            Result.success(post.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePosts(limit: Int = 50): Flow<List<Post>> = callbackFlow {
        val listener = db.collection(POSTS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { it.toPost() } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    suspend fun togglePostLike(postId: String, userId: String, isLiked: Boolean): Result<Unit> {
        return try {
            val postRef = db.collection(POSTS).document(postId)
            if (isLiked) {
                postRef.update(
                    mapOf(
                        "likeCount" to FieldValue.increment(1),
                        "likedBy" to FieldValue.arrayUnion(userId)
                    )
                ).await()
            } else {
                postRef.update(
                    mapOf(
                        "likeCount" to FieldValue.increment(-1),
                        "likedBy" to FieldValue.arrayRemove(userId)
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== STORY OPERATIONS ====================

    suspend fun createStory(story: Story): Result<String> {
        return try {
            db.collection(STORIES).document(story.id).set(story.toMap()).await()
            Result.success(story.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeStories(): Flow<List<Story>> = callbackFlow {
        val listener = db.collection(STORIES)
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val stories = snapshot?.documents?.mapNotNull { it.toStory() } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markStoryViewed(storyId: String, userId: String): Result<Unit> {
        return try {
            db.collection(STORIES).document(storyId).update(
                mapOf(
                    "viewedBy" to FieldValue.arrayUnion(userId),
                    "viewCount" to FieldValue.increment(1)
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== SHARED SPACE OPERATIONS ====================

    suspend fun createSharedSpace(space: SharedSpace): Result<String> {
        return try {
            db.collection(SHARED_SPACES).document(space.id).set(space.toMap()).await()
            Result.success(space.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSharedSpaces(userId: String): Flow<List<SharedSpace>> = callbackFlow {
        val listener = db.collection(SHARED_SPACES)
            .whereArrayContains("memberIds", userId)
            .orderBy("lastActivityAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val spaces = snapshot?.documents?.mapNotNull { it.toSharedSpace() } ?: emptyList()
                trySend(spaces)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addMediaToSpace(spaceId: String, media: Media): Result<String> {
        return try {
            db.collection(SHARED_SPACES)
                .document(spaceId)
                .collection(MEDIA)
                .document(media.id)
                .set(media.toMap())
                .await()

            db.collection(SHARED_SPACES).document(spaceId).update(
                mapOf(
                    "mediaCount" to FieldValue.increment(1),
                    "lastActivityAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(media.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== MESSAGE ACK OPERATIONS ====================

    suspend fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus
    ): Result<Unit> {
        return try {
            db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .document(messageId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Marque tous les messages non-lus d'un expéditeur comme DELIVERED dans une conversation
    suspend fun markConversationMessagesDelivered(
        conversationId: String,
        currentUserId: String
    ): Result<Unit> {
        return try {
            val batch = db.batch()
            val messages = db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .whereNotEqualTo("senderId", currentUserId)
                .whereEqualTo("status", MessageStatus.SENT.name)
                .get()
                .await()
            messages.documents.forEach { doc ->
                batch.update(doc.reference, "status", MessageStatus.DELIVERED.name)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Marks unread messages as READ when the conversation is opened.
    // Uses chunked batches (Firestore limit: 500 operations per batch).
    suspend fun markConversationMessagesRead(
        conversationId: String,
        currentUserId: String
    ): Result<Unit> {
        return try {
            val messages = db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .whereNotEqualTo("senderId", currentUserId)
                .get()
                .await()

            val unreadDocs = messages.documents
                .filter { it.getString("status") != MessageStatus.READ.name }

            // Firestore batch limit is 500 operations
            unreadDocs.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, "status", MessageStatus.READ.name)
                }
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== GROUP ADMIN OPERATIONS ====================

    suspend fun promoteToAdmin(conversationId: String, userId: String): Result<Unit> {
        return try {
            db.collection(CONVERSATIONS).document(conversationId).update(
                "adminIds", FieldValue.arrayUnion(userId)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun demoteAdmin(conversationId: String, userId: String): Result<Unit> {
        return try {
            db.collection(CONVERSATIONS).document(conversationId).update(
                "adminIds", FieldValue.arrayRemove(userId)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateInviteLink(conversationId: String): Result<String> {
        return try {
            val link = "memoryshare://join/$conversationId/${UUID.randomUUID()}"
            db.collection(CONVERSATIONS).document(conversationId).update(
                "inviteLink", link
            ).await()
            Result.success(link)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== EPHEMERAL MESSAGES OPERATIONS ====================

    suspend fun setEphemeralDuration(
        conversationId: String,
        durationMs: Long?
    ): Result<Unit> {
        return try {
            db.collection(CONVERSATIONS).document(conversationId).update(
                "ephemeralDuration", durationMs
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpiredMessages(conversationId: String): Result<Int> {
        return try {
            val now = System.currentTimeMillis()
            val expired = db.collection(CONVERSATIONS)
                .document(conversationId)
                .collection(MESSAGES)
                .whereLessThan("expiresAt", now)
                .get()
                .await()
            val batch = db.batch()
            expired.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(expired.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PRIVACY SETTINGS OPERATIONS ====================

    suspend fun savePrivacySettings(userId: String, settings: PrivacySettings): Result<Unit> {
        return try {
            db.collection(PRESENCE).document(userId).set(
                mapOf("privacySettings" to settings.toMap()),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrivacySettings(userId: String): Result<PrivacySettings> {
        return try {
            val doc = db.collection(PRESENCE).document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            val map = doc.get("privacySettings") as? Map<String, Any?> ?: emptyMap()
            Result.success(PrivacySettings.fromMap(map))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PRESENCE OPERATIONS ====================

    suspend fun updatePresence(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            db.collection(PRESENCE).document(userId).set(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePresence(userId: String): Flow<Pair<Boolean, Long>> = callbackFlow {
        val listener = db.collection(PRESENCE).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val isOnline = snapshot?.getBoolean("isOnline") ?: false
                val lastSeen = snapshot?.getLong("lastSeen") ?: 0L
                trySend(Pair(isOnline, lastSeen))
            }
        awaitClose { listener.remove() }
    }

    // ==================== FCM TOKEN OPERATIONS ====================

    suspend fun saveFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            db.collection(FCM_TOKENS).document(userId).set(
                mapOf(
                    "token" to token,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFcmToken(userId: String): Result<String?> {
        return try {
            val doc = db.collection(FCM_TOKENS).document(userId).get().await()
            Result.success(doc.getString("token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== FOLLOW OPERATIONS ====================

    suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            val followId = "${followerId}_${followingId}"
            db.collection(FOLLOWS).document(followId).set(
                mapOf(
                    "followerId" to followerId,
                    "followingId" to followingId,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            val followId = "${followerId}_${followingId}"
            db.collection(FOLLOWS).document(followId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> {
        return try {
            val followId = "${followerId}_${followingId}"
            val doc = db.collection(FOLLOWS).document(followId).get().await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== EXTENSION FUNCTIONS ====================

    private fun User.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "username" to username,
        "displayName" to displayName,
        "email" to email,
        "profilePictureUrl" to profilePictureUrl,
        "bio" to bio,
        "createdAt" to createdAt
    )

    private fun Conversation.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "isGroup" to isGroup,
        "participantIds" to participantIds,
        "lastMessageText" to lastMessageText,
        "lastMessageTime" to lastMessageTime,
        "imageUrl" to imageUrl,
        "createdAt" to createdAt,
        "archived" to archived,
        "pinned" to pinned,
        "pinnedAt" to pinnedAt,
        "muted" to muted,
        "adminIds" to adminIds,
        "inviteLink" to inviteLink,
        "ephemeralDuration" to ephemeralDuration
    )

    private fun Message.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "conversationId" to conversationId,
        "senderId" to senderId,
        "content" to content,
        "type" to type.name,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "isStarred" to isStarred,
        "mediaUrl" to mediaUrl,
        "mediaThumbnailUrl" to mediaThumbnailUrl,
        "mediaDuration" to mediaDuration,
        "replyToId" to replyToId,
        "editedAt" to editedAt,
        "status" to status.name,
        "expiresAt" to expiresAt
    )

    private fun Post.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "authorId" to authorId,
        "caption" to caption,
        "mediaUrls" to mediaUrls,
        "mediaType" to mediaType.name,
        "timestamp" to timestamp,
        "likeCount" to likeCount,
        "commentCount" to commentCount,
        "visibility" to visibility.name
    )

    private fun Story.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "authorId" to authorId,
        "mediaUrl" to mediaUrl,
        "mediaType" to mediaType.name,
        "createdAt" to createdAt,
        "expiresAt" to expiresAt,
        "viewedBy" to viewedBy,
        "viewCount" to viewCount
    )

    private fun SharedSpace.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "creatorId" to creatorId,
        "memberIds" to memberIds,
        "coverImageUrl" to coverImageUrl,
        "createdAt" to createdAt,
        "lastActivityAt" to lastActivityAt,
        "mediaCount" to mediaCount
    )


    private fun Media.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "spaceId" to spaceId,
        "uploaderId" to uploaderId,
        "url" to url,
        "thumbnailUrl" to thumbnailUrl,
        "type" to type.name,
        "title" to title,
        "description" to description,
        "createdAt" to createdAt,
        "capturedAt" to capturedAt,
        "duration" to duration,
        "width" to width,
        "height" to height,
        "fileSize" to fileSize,
        "likeCount" to likeCount
    )
}
