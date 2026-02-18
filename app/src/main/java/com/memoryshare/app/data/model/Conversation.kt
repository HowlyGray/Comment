package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String = "",
    val name: String? = null, // Pour les groupes
    val isGroup: Boolean = false,
    val participantIds: List<String> = emptyList(), // Liste des IDs des participants
    val lastMessageText: String? = null,
    val lastMessageTime: Long? = null,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val pinnedAt: Long? = null, // Timestamp quand la conversation a été épinglée
    val muted: Boolean = false,
    // Nouveau : rôles admin dans les groupes
    val adminIds: List<String> = emptyList(),
    // Nouveau : lien d'invitation pour les groupes
    val inviteLink: String? = null,
    // Nouveau : durée des messages éphémères (null = désactivé, en ms)
    // 86_400_000 = 24h | 604_800_000 = 7j | 7_776_000_000 = 90j
    val ephemeralDuration: Long? = null
)
