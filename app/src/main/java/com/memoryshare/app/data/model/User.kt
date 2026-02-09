package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val profilePictureUrl: String? = null,
    val bio: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
