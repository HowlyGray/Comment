package com.memoryshare.app.data.local

import androidx.room.TypeConverter
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.MessageStatus
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.data.model.PostMediaType
import com.memoryshare.app.data.model.PermissionLevel
import com.memoryshare.app.data.model.StoryMediaType
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        // Handle legacy comma-separated format for backward compatibility
        return try {
            val jsonArray = JSONArray(value)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            // Fallback for old comma-separated data
            value.split(",").filter { it.isNotEmpty() }
        }
    }

    @TypeConverter
    fun fromMessageType(value: MessageType): String {
        return value.name
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType {
        return MessageType.valueOf(value)
    }

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String {
        return value.name
    }

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus {
        return try { MessageStatus.valueOf(value) } catch (_: Exception) { MessageStatus.SENT }
    }

    @TypeConverter
    fun fromMediaType(value: MediaType): String {
        return value.name
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return MediaType.valueOf(value)
    }

    @TypeConverter
    fun fromPostMediaType(value: PostMediaType): String {
        return value.name
    }

    @TypeConverter
    fun toPostMediaType(value: String): PostMediaType {
        return PostMediaType.valueOf(value)
    }

    @TypeConverter
    fun fromPermissionLevel(value: PermissionLevel): String {
        return value.name
    }

    @TypeConverter
    fun toPermissionLevel(value: String): PermissionLevel {
        return PermissionLevel.valueOf(value)
    }

    @TypeConverter
    fun fromStoryMediaType(value: StoryMediaType): String {
        return value.name
    }

    @TypeConverter
    fun toStoryMediaType(value: String): StoryMediaType {
        return StoryMediaType.valueOf(value)
    }
}
