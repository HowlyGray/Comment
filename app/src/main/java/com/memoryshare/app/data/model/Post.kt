package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PostMediaType {
    IMAGE,
    VIDEO,
    AUDIO
}

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String,
    val authorId: String,
    val caption: String? = null,
    val mediaUrls: List<String>, // Liste des URLs des médias
    val mediaType: PostMediaType,
    val thumbnailUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isLikedByCurrentUser: Boolean = false
)
