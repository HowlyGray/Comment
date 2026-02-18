package com.memoryshare.app.data.model

// Qui peut voir cette information
enum class PrivacyVisibility {
    EVERYONE,  // Tout le monde
    CONTACTS,  // Mes contacts uniquement
    NOBODY     // Personne
}

data class PrivacySettings(
    val showLastSeen: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val showProfilePicture: PrivacyVisibility = PrivacyVisibility.EVERYONE,
    val showOnlineStatus: PrivacyVisibility = PrivacyVisibility.EVERYONE
) {
    fun toMap(): Map<String, String> = mapOf(
        "showLastSeen" to showLastSeen.name,
        "showProfilePicture" to showProfilePicture.name,
        "showOnlineStatus" to showOnlineStatus.name
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): PrivacySettings {
            fun parseVisibility(key: String): PrivacyVisibility =
                try { PrivacyVisibility.valueOf(map[key] as? String ?: "") }
                catch (_: Exception) { PrivacyVisibility.EVERYONE }

            return PrivacySettings(
                showLastSeen = parseVisibility("showLastSeen"),
                showProfilePicture = parseVisibility("showProfilePicture"),
                showOnlineStatus = parseVisibility("showOnlineStatus")
            )
        }
    }
}
