package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_spaces")
data class SharedSpace(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val creatorId: String = "",
    val coverImageUrl: String? = null,
    val memberIds: List<String> = emptyList(),
    val mediaCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis()
)
