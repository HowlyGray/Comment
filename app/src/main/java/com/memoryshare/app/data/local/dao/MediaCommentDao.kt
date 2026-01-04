package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.MediaComment
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaCommentDao {
    @Query("SELECT * FROM media_comments WHERE mediaId = :mediaId ORDER BY timestamp ASC")
    fun getCommentsByMedia(mediaId: String): Flow<List<MediaComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: MediaComment)

    @Delete
    suspend fun deleteComment(comment: MediaComment)

    @Query("DELETE FROM media_comments WHERE mediaId = :mediaId")
    suspend fun deleteCommentsByMedia(mediaId: String)
}
