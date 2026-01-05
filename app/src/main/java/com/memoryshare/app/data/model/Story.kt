package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StoryMediaType {
    IMAGE,
    VIDEO
}

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey
    val id: String,
    val authorId: String,
    val mediaUrl: String,
    val mediaType: StoryMediaType,
    val thumbnailUrl: String? = null,
    val caption: String? = null,
    val duration: Long? = null, // Pour vidéo en millisecondes
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 heures
    val viewCount: Int = 0,
    val viewedBy: List<String> = emptyList() // Liste des IDs des utilisateurs ayant vu
)
