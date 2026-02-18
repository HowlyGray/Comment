package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE
}

// États ACK WhatsApp-style : envoyé → délivré → lu
enum class MessageStatus {
    SENDING,   // En cours d'envoi (local uniquement)
    SENT,      // ✓  Reçu par le serveur
    DELIVERED, // ✓✓ Reçu sur l'appareil du destinataire
    READ       // ✓✓ (bleu) Lu par le destinataire
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val content: String = "", // Texte ou URL du média
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isRead")
    val isRead: Boolean = false,
    @get:PropertyName("isStarred")
    val isStarred: Boolean = false, // Message marqué comme important
    val mediaUrl: String? = null,
    val mediaThumbnailUrl: String? = null,
    val mediaDuration: Long? = null, // Pour audio/vidéo en millisecondes
    val replyToId: String? = null, // ID du message auquel on répond
    val editedAt: Long? = null, // Timestamp de la dernière édition
    // Nouveau : ACK 3 états
    val status: MessageStatus = MessageStatus.SENT,
    // Nouveau : messages éphémères (null = pas d'expiration)
    val expiresAt: Long? = null
)
