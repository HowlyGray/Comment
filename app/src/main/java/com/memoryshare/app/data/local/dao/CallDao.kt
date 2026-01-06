package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Call
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<Call>>

    @Query("SELECT * FROM calls WHERE userId = :userId ORDER BY timestamp DESC")
    fun getCallsByUser(userId: String): Flow<List<Call>>

    @Query("SELECT * FROM calls WHERE id = :callId")
    fun getCallById(callId: String): Flow<Call?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: Call)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<Call>)

    @Update
    suspend fun updateCall(call: Call)

    @Delete
    suspend fun deleteCall(call: Call)

    @Query("DELETE FROM calls WHERE userId = :userId")
    suspend fun deleteCallsByUser(userId: String)

    @Query("DELETE FROM calls")
    suspend fun deleteAllCalls()
}
