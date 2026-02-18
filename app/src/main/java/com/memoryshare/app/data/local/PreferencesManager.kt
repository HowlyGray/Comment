package com.memoryshare.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.memoryshare.app.data.model.PrivacySettings
import com.memoryshare.app.data.model.PrivacyVisibility

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "memoryshare_prefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_FAB_ON_LEFT = "fab_on_left"
        private const val KEY_DEFAULT_POST_VISIBILITY = "default_post_visibility"
        // Confidentialité
        private const val KEY_PRIVACY_LAST_SEEN = "privacy_last_seen"
        private const val KEY_PRIVACY_PROFILE_PIC = "privacy_profile_picture"
        private const val KEY_PRIVACY_ONLINE_STATUS = "privacy_online_status"
    }

    fun saveCurrentUserId(userId: String?) {
        prefs.edit().apply {
            if (userId != null) {
                putString(KEY_CURRENT_USER_ID, userId)
            } else {
                remove(KEY_CURRENT_USER_ID)
            }
            apply()
        }
    }

    fun getCurrentUserId(): String? {
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }

    fun clearCurrentUser() {
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }

    fun setFabOnLeft(onLeft: Boolean) {
        prefs.edit().putBoolean(KEY_FAB_ON_LEFT, onLeft).apply()
    }

    fun isFabOnLeft(): Boolean {
        return prefs.getBoolean(KEY_FAB_ON_LEFT, false)
    }

    fun setDefaultPostVisibility(visibility: String) {
        prefs.edit().putString(KEY_DEFAULT_POST_VISIBILITY, visibility).apply()
    }

    fun getDefaultPostVisibility(): String {
        return prefs.getString(KEY_DEFAULT_POST_VISIBILITY, "PUBLIC") ?: "PUBLIC"
    }

    // ==================== CONFIDENTIALITÉ ====================

    fun savePrivacySettings(settings: PrivacySettings) {
        prefs.edit().apply {
            putString(KEY_PRIVACY_LAST_SEEN, settings.showLastSeen.name)
            putString(KEY_PRIVACY_PROFILE_PIC, settings.showProfilePicture.name)
            putString(KEY_PRIVACY_ONLINE_STATUS, settings.showOnlineStatus.name)
            apply()
        }
    }

    fun getPrivacySettings(): PrivacySettings {
        fun loadVisibility(key: String): PrivacyVisibility =
            try { PrivacyVisibility.valueOf(prefs.getString(key, PrivacyVisibility.EVERYONE.name) ?: "") }
            catch (_: Exception) { PrivacyVisibility.EVERYONE }

        return PrivacySettings(
            showLastSeen = loadVisibility(KEY_PRIVACY_LAST_SEEN),
            showProfilePicture = loadVisibility(KEY_PRIVACY_PROFILE_PIC),
            showOnlineStatus = loadVisibility(KEY_PRIVACY_ONLINE_STATUS)
        )
    }
}
