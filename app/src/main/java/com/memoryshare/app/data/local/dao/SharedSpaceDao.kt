package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.SharedSpace
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedSpaceDao {
    @Query("SELECT * FROM shared_spaces ORDER BY lastActivityAt DESC")
    fun getAllSharedSpaces(): Flow<List<SharedSpace>>

    @Query("SELECT * FROM shared_spaces WHERE id = :spaceId")
    fun getSharedSpaceById(spaceId: String): Flow<SharedSpace?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedSpace(space: SharedSpace)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedSpaces(spaces: List<SharedSpace>)

    @Update
    suspend fun updateSharedSpace(space: SharedSpace)

    @Delete
    suspend fun deleteSharedSpace(space: SharedSpace)

    @Query("DELETE FROM shared_spaces")
    suspend fun deleteAllSharedSpaces()

    @Query("SELECT * FROM shared_spaces WHERE id = :spaceId")
    suspend fun getSharedSpaceByIdSync(spaceId: String): SharedSpace?

    @Query("SELECT * FROM shared_spaces")
    suspend fun getAllSharedSpacesSync(): List<SharedSpace>
}
