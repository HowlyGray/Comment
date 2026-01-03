package com.memoryshare.app.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "memoryshare_prefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
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
}
