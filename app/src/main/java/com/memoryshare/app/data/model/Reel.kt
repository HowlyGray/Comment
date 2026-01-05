package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reels")
data class Reel(
    @PrimaryKey
    val id: String,
    val authorId: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val caption: String? = null,
    val duration: Long, // Durée en millisecondes
    val timestamp: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val viewCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val audioUrl: String? = null, // URL de l'audio original
    val audioName: String? = null // Nom de l'audio
)
