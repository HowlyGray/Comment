package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Media
import com.memoryshare.app.data.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE spaceId = :spaceId ORDER BY createdAt DESC")
    fun getMediaBySpace(spaceId: String): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE spaceId = :spaceId AND type = :type ORDER BY createdAt DESC")
    fun getMediaBySpaceAndType(spaceId: String, type: MediaType): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE id = :mediaId")
    fun getMediaById(mediaId: String): Flow<Media?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: Media)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<Media>)

    @Update
    suspend fun updateMedia(media: Media)

    @Delete
    suspend fun deleteMedia(media: Media)

    @Query("DELETE FROM media WHERE spaceId = :spaceId")
    suspend fun deleteMediaBySpace(spaceId: String)

    @Query("SELECT COUNT(*) FROM media WHERE spaceId = :spaceId")
    suspend fun getMediaCountForSpace(spaceId: String): Int
}
