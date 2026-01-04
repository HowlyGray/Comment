package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey
    val id: String,
    val postId: String,
    val authorId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likeCount: Int = 0,
    val likedByCurrentUser: Boolean = false
)
