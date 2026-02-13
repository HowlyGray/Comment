package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}

@Entity(tableName = "media")
data class Media(
    @PrimaryKey
    val id: String = "",
    val spaceId: String = "",
    val uploaderId: String = "",
    val url: String = "",
    val thumbnailUrl: String? = null,
    val type: MediaType = MediaType.IMAGE,
    val title: String? = null,
    val description: String? = null,
    val duration: Long? = null, // Pour vidéo/audio en millisecondes
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null, // En octets
    val createdAt: Long = System.currentTimeMillis(),
    val capturedAt: Long? = null, // Date de capture si disponible
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByCurrentUser: Boolean = false
)
