package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PermissionLevel {
    READ,   // Peut seulement voir
    WRITE   // Peut voir et ajouter/supprimer
}

@Entity(
    tableName = "shared_space_permissions",
    indices = [
        Index(value = ["spaceId", "userId"], unique = true),
        Index(value = ["spaceId"])
    ]
)
data class SharedSpacePermission(
    @PrimaryKey
    val id: String = "",
    val spaceId: String = "",
    val userId: String = "",
    val permission: PermissionLevel = PermissionLevel.READ,
    val grantedAt: Long = System.currentTimeMillis()
)
