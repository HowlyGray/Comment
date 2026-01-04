package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.PermissionLevel
import com.memoryshare.app.data.model.SharedSpacePermission
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedSpacePermissionDao {
    @Query("SELECT * FROM shared_space_permissions WHERE spaceId = :spaceId")
    fun getPermissionsForSpace(spaceId: String): Flow<List<SharedSpacePermission>>

    @Query("SELECT * FROM shared_space_permissions WHERE spaceId = :spaceId AND userId = :userId")
    fun getUserPermission(spaceId: String, userId: String): Flow<SharedSpacePermission?>

    @Query("SELECT permission FROM shared_space_permissions WHERE spaceId = :spaceId AND userId = :userId")
    suspend fun getUserPermissionLevel(spaceId: String, userId: String): PermissionLevel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: SharedSpacePermission)

    @Delete
    suspend fun deletePermission(permission: SharedSpacePermission)

    @Query("DELETE FROM shared_space_permissions WHERE spaceId = :spaceId AND userId = :userId")
    suspend fun deleteUserPermission(spaceId: String, userId: String)

    @Query("DELETE FROM shared_space_permissions WHERE spaceId = :spaceId")
    suspend fun deleteAllPermissionsForSpace(spaceId: String)
}
