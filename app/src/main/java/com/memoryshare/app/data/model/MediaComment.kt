package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_comments")
data class MediaComment(
    @PrimaryKey
    val id: String,
    val mediaId: String,
    val authorId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
