package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_reactions",
    indices = [
        Index(value = ["messageId", "userId"], unique = true),
        Index(value = ["messageId"])
    ]
)
data class MessageReaction(
    @PrimaryKey
    val id: String,
    val messageId: String,
    val userId: String,
    val emoji: String, // L'emoji (ex: "👍", "❤️", "😂")
    val timestamp: Long = System.currentTimeMillis()
)
