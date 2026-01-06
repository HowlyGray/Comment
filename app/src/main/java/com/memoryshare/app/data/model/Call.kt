package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallType {
    VOICE,
    VIDEO
}

enum class CallStatus {
    OUTGOING,
    INCOMING,
    MISSED,
    DECLINED,
    ENDED
}

@Entity(tableName = "calls")
data class Call(
    @PrimaryKey
    val id: String,
    val userId: String, // ID de l'utilisateur appelé/appelant
    val type: CallType,
    val status: CallStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long? = null, // Durée en secondes (null si appel manqué/décliné)
    val isIncoming: Boolean // true = appel entrant, false = appel sortant
)
