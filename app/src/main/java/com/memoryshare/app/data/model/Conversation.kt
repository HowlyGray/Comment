package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String,
    val name: String? = null, // Pour les groupes
    val isGroup: Boolean = false,
    val participantIds: List<String>, // Liste des IDs des participants
    val lastMessageText: String? = null,
    val lastMessageTime: Long? = null,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val pinnedAt: Long? = null, // Timestamp quand la conversation a été épinglée
    val muted: Boolean = false
)
