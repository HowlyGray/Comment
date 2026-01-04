package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_follows",
    indices = [
        Index(value = ["followerId", "followingId"], unique = true),
        Index(value = ["followerId"]),
        Index(value = ["followingId"])
    ]
)
data class UserFollow(
    @PrimaryKey
    val id: String,
    val followerId: String,  // User qui suit
    val followingId: String, // User qui est suivi
    val createdAt: Long = System.currentTimeMillis()
)
