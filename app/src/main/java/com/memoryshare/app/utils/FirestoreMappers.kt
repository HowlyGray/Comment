package com.memoryshare.app.utils

import com.google.firebase.firestore.DocumentSnapshot
import com.memoryshare.app.data.model.*

/**
 * Centralized Firestore DocumentSnapshot to Model mappers.
 * Used across FirestoreManager, RealtimeSyncManager, and MessageRepository
 * to avoid duplication and keep mappings consistent.
 */
object FirestoreMappers {

    fun DocumentSnapshot.toUser(): User? {
        return try {
            User(
                id = getString("id") ?: id,
                username = getString("username") ?: "",
                displayName = getString("displayName") ?: "",
                email = getString("email") ?: "",
                profilePictureUrl = getString("profilePictureUrl"),
                bio = getString("bio"),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun DocumentSnapshot.toConversation(): Conversation? {
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
                muted = getBoolean("muted") ?: false,
                adminIds = get("adminIds") as? List<String> ?: emptyList(),
                inviteLink = getString("inviteLink"),
                ephemeralDuration = getLong("ephemeralDuration")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun DocumentSnapshot.toMessage(): Message? {
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
                editedAt = getLong("editedAt"),
                status = try {
                    MessageStatus.valueOf(getString("status") ?: "SENT")
                } catch (_: Exception) { MessageStatus.SENT },
                expiresAt = getLong("expiresAt")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun DocumentSnapshot.toPost(): Post? {
        return try {
            @Suppress("UNCHECKED_CAST")
            Post(
                id = getString("id") ?: id,
                authorId = getString("authorId") ?: "",
                caption = getString("caption"),
                mediaUrls = get("mediaUrls") as? List<String> ?: emptyList(),
                mediaType = PostMediaType.valueOf(getString("mediaType") ?: "IMAGE"),
                timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
                likeCount = getLong("likeCount")?.toInt() ?: 0,
                commentCount = getLong("commentCount")?.toInt() ?: 0,
                visibility = try {
                    PostVisibility.valueOf(getString("visibility") ?: "PUBLIC")
                } catch (_: Exception) {
                    PostVisibility.PUBLIC
                }
            )
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun DocumentSnapshot.toStory(): Story? {
        return try {
            Story(
                id = getString("id") ?: id,
                authorId = getString("authorId") ?: "",
                mediaUrl = getString("mediaUrl") ?: "",
                mediaType = StoryMediaType.valueOf(getString("mediaType") ?: "IMAGE"),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                expiresAt = getLong("expiresAt") ?: System.currentTimeMillis(),
                viewedBy = get("viewedBy") as? List<String> ?: emptyList(),
                viewCount = getLong("viewCount")?.toInt() ?: 0
            )
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun DocumentSnapshot.toSharedSpace(): SharedSpace? {
        return try {
            SharedSpace(
                id = getString("id") ?: id,
                name = getString("name") ?: "",
                description = getString("description"),
                creatorId = getString("creatorId") ?: "",
                memberIds = get("memberIds") as? List<String> ?: emptyList(),
                coverImageUrl = getString("coverImageUrl"),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                lastActivityAt = getLong("lastActivityAt") ?: System.currentTimeMillis(),
                mediaCount = getLong("mediaCount")?.toInt() ?: 0
            )
        } catch (_: Exception) {
            null
        }
    }
}
