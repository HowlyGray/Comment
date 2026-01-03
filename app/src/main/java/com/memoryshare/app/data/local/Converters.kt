package com.memoryshare.app.data.local

import androidx.room.TypeConverter
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.data.model.PostMediaType

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
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
}
