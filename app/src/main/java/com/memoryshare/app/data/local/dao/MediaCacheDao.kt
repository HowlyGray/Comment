package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.MediaCache
import com.memoryshare.app.data.model.MediaSourceType
import com.memoryshare.app.data.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaCacheDao {

    @Query("SELECT * FROM media_cache ORDER BY createdAt DESC")
    fun getAllMediaCache(): Flow<List<MediaCache>>

    @Query("SELECT * FROM media_cache WHERE id = :id")
    suspend fun getMediaCacheById(id: String): MediaCache?

    @Query("SELECT * FROM media_cache WHERE id = :id")
    fun getMediaCacheByIdFlow(id: String): Flow<MediaCache?>

    @Query("SELECT * FROM media_cache WHERE sourceType = :sourceType AND sourceId = :sourceId")
    fun getMediaBySource(sourceType: MediaSourceType, sourceId: String): Flow<List<MediaCache>>

    @Query("SELECT * FROM media_cache WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun getMediaBySourceSync(sourceType: MediaSourceType, sourceId: String): List<MediaCache>

    @Query("SELECT * FROM media_cache WHERE type = :type ORDER BY createdAt DESC")
    fun getMediaByType(type: MediaType): Flow<List<MediaCache>>

    @Query("SELECT * FROM media_cache WHERE isSynced = 0")
    suspend fun getUnsyncedMedia(): List<MediaCache>

    @Query("SELECT * FROM media_cache WHERE isDownloaded = 0")
    suspend fun getNotDownloadedMedia(): List<MediaCache>

    @Query("SELECT * FROM media_cache WHERE firebaseUrl = :firebaseUrl LIMIT 1")
    suspend fun getMediaByFirebaseUrl(firebaseUrl: String): MediaCache?

    @Query("SELECT SUM(size) FROM media_cache")
    suspend fun getTotalCacheSize(): Long?

    @Query("DELETE FROM media_cache WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaCache: MediaCache)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mediaCache: List<MediaCache>)

    @Update
    suspend fun update(mediaCache: MediaCache)

    @Delete
    suspend fun delete(mediaCache: MediaCache)

    @Query("DELETE FROM media_cache WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE media_cache SET isSynced = 1, firebaseUrl = :firebaseUrl WHERE id = :id")
    suspend fun markAsSynced(id: String, firebaseUrl: String)

    @Query("UPDATE media_cache SET isDownloaded = 1, localPath = :localPath WHERE id = :id")
    suspend fun markAsDownloaded(id: String, localPath: String)
}
