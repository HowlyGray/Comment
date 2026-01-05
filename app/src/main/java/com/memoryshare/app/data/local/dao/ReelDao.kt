package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Reel
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {
    @Query("SELECT * FROM reels ORDER BY timestamp DESC")
    fun getAllReels(): Flow<List<Reel>>

    @Query("SELECT * FROM reels WHERE authorId = :userId ORDER BY timestamp DESC")
    fun getReelsByUser(userId: String): Flow<List<Reel>>

    @Query("SELECT * FROM reels WHERE id = :reelId")
    fun getReelById(reelId: String): Flow<Reel?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(reel: Reel)

    @Update
    suspend fun updateReel(reel: Reel)

    @Delete
    suspend fun deleteReel(reel: Reel)

    @Query("DELETE FROM reels WHERE authorId = :userId")
    suspend fun deleteReelsByUser(userId: String)
}
