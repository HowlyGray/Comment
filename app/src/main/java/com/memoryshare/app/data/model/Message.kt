package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String, // Texte ou URL du média
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isStarred: Boolean = false, // Message marqué comme important
    val mediaUrl: String? = null,
    val mediaThumbnailUrl: String? = null,
    val mediaDuration: Long? = null, // Pour audio/vidéo en millisecondes
    val replyToId: String? = null, // ID du message auquel on répond
    val editedAt: Long? = null // Timestamp de la dernière édition
)
