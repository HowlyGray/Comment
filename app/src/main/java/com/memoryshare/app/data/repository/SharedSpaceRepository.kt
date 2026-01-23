package com.memoryshare.app.data.repository

import android.util.Log
import com.memoryshare.app.data.local.dao.MediaDao
import com.memoryshare.app.data.local.dao.SharedSpaceDao
import com.memoryshare.app.data.local.dao.SharedSpacePermissionDao
import com.memoryshare.app.data.model.Media
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.SharedSpace
import com.memoryshare.app.data.model.SharedSpacePermission
import com.memoryshare.app.utils.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class SharedSpaceRepository(
    private val sharedSpaceDao: SharedSpaceDao,
    private val mediaDao: MediaDao,
    private val permissionDao: SharedSpacePermissionDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore = FirebaseManager.firestore

    companion object {
        private const val TAG = "SharedSpaceRepository"
    }

    fun getAllSharedSpaces(): Flow<List<SharedSpace>> = sharedSpaceDao.getAllSharedSpaces()

    fun getSharedSpaceById(spaceId: String): Flow<SharedSpace?> =
        sharedSpaceDao.getSharedSpaceById(spaceId)

    fun getMediaBySpace(spaceId: String): Flow<List<Media>> =
        mediaDao.getMediaBySpace(spaceId)

    fun getMediaBySpaceAndType(spaceId: String, type: MediaType): Flow<List<Media>> =
        mediaDao.getMediaBySpaceAndType(spaceId, type)

    suspend fun createSharedSpace(
        name: String,
        creatorId: String,
        description: String? = null,
        memberIds: List<String> = emptyList()
    ): SharedSpace {
        val space = SharedSpace(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            creatorId = creatorId,
            memberIds = memberIds + creatorId // Le créateur est toujours membre
        )
        // Sauvegarder localement
        sharedSpaceDao.insertSharedSpace(space)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.SHARED_SPACES,
                documentId = space.id,
                data = space
            ).onSuccess {
                Log.d(TAG, "Shared space synced to Firebase: ${space.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync shared space to Firebase: ${space.id}", e)
            }
        }

        return space
    }

    suspend fun addMedia(
        spaceId: String,
        uploaderId: String,
        url: String,
        type: MediaType,
        title: String? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        duration: Long? = null
    ): Media {
        val media = Media(
            id = UUID.randomUUID().toString(),
            spaceId = spaceId,
            uploaderId = uploaderId,
            url = url,
            type = type,
            title = title,
            description = description,
            thumbnailUrl = thumbnailUrl,
            duration = duration
        )
        // Sauvegarder localement
        mediaDao.insertMedia(media)

        // Mettre à jour le nombre de médias et la dernière activité de l'espace
        val space = sharedSpaceDao.getSharedSpaceById(spaceId).firstOrNull()
        space?.let {
            val updatedSpace = it.copy(
                mediaCount = it.mediaCount + 1,
                lastActivityAt = System.currentTimeMillis()
            )
            sharedSpaceDao.updateSharedSpace(updatedSpace)

            // Synchroniser avec Firebase
            scope.launch {
                // Sauvegarder le média
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.MEDIA,
                    documentId = media.id,
                    data = media
                ).onSuccess {
                    Log.d(TAG, "Media synced to Firebase: ${media.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to sync media to Firebase: ${media.id}", e)
                }

                // Mettre à jour l'espace partagé
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.SHARED_SPACES,
                    documentId = updatedSpace.id,
                    data = updatedSpace
                ).onSuccess {
                    Log.d(TAG, "Shared space updated in Firebase: ${updatedSpace.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to update shared space in Firebase: ${updatedSpace.id}", e)
                }
            }
        }

        return media
    }

    suspend fun addMemberToSpace(spaceId: String, userId: String) {
        val space = sharedSpaceDao.getSharedSpaceById(spaceId).firstOrNull()
        space?.let {
            if (!it.memberIds.contains(userId)) {
                sharedSpaceDao.updateSharedSpace(
                    it.copy(memberIds = it.memberIds + userId)
                )
            }
        }
    }

    suspend fun removeMemberFromSpace(spaceId: String, userId: String) {
        val space = sharedSpaceDao.getSharedSpaceById(spaceId).firstOrNull()
        space?.let {
            sharedSpaceDao.updateSharedSpace(
                it.copy(memberIds = it.memberIds.filter { id -> id != userId })
            )
        }
    }

    suspend fun deleteMedia(media: Media) {
        // Supprimer localement
        mediaDao.deleteMedia(media)

        // Mettre à jour le nombre de médias de l'espace
        val space = sharedSpaceDao.getSharedSpaceById(media.spaceId).firstOrNull()
        space?.let {
            val updatedSpace = it.copy(mediaCount = (it.mediaCount - 1).coerceAtLeast(0))
            sharedSpaceDao.updateSharedSpace(updatedSpace)

            // Synchroniser avec Firebase
            scope.launch {
                // Supprimer le média
                FirebaseManager.deleteDocument(
                    collection = FirebaseManager.Collections.MEDIA,
                    documentId = media.id
                ).onSuccess {
                    Log.d(TAG, "Media deleted from Firebase: ${media.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to delete media from Firebase: ${media.id}", e)
                }

                // Mettre à jour l'espace
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.SHARED_SPACES,
                    documentId = updatedSpace.id,
                    data = updatedSpace
                ).onSuccess {
                    Log.d(TAG, "Shared space updated in Firebase: ${updatedSpace.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to update shared space in Firebase: ${updatedSpace.id}", e)
                }
            }
        }
    }

    suspend fun deleteSharedSpace(space: SharedSpace) {
        // Supprimer localement
        mediaDao.deleteMediaBySpace(space.id)
        sharedSpaceDao.deleteSharedSpace(space)

        // Supprimer de Firebase
        scope.launch {
            FirebaseManager.deleteDocument(
                collection = FirebaseManager.Collections.SHARED_SPACES,
                documentId = space.id
            ).onSuccess {
                Log.d(TAG, "Shared space deleted from Firebase: ${space.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete shared space from Firebase: ${space.id}", e)
            }
        }
    }

    fun getSpacePermissions(spaceId: String): Flow<List<SharedSpacePermission>> =
        permissionDao.getPermissionsForSpace(spaceId)

    suspend fun updatePermission(permission: SharedSpacePermission) {
        // Sauvegarder localement
        permissionDao.insertPermission(permission)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = "shared_space_permissions",
                documentId = permission.id,
                data = permission
            ).onSuccess {
                Log.d(TAG, "Permission synced to Firebase: ${permission.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync permission to Firebase: ${permission.id}", e)
            }
        }
    }

    /**
     * Synchronise les espaces partagés depuis Firebase vers la base de données locale
     */
    suspend fun syncSharedSpacesFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.SHARED_SPACES,
            clazz = SharedSpace::class.java
        ).onSuccess { spaces ->
            spaces.forEach { space ->
                sharedSpaceDao.insertSharedSpace(space)
            }
            Log.d(TAG, "Synced ${spaces.size} shared spaces from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync shared spaces from Firebase", e)
        }
    }

    /**
     * Synchronise les médias depuis Firebase vers la base de données locale
     */
    suspend fun syncMediaFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.MEDIA,
            clazz = Media::class.java
        ).onSuccess { mediaList ->
            mediaList.forEach { media ->
                mediaDao.insertMedia(media)
            }
            Log.d(TAG, "Synced ${mediaList.size} media from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync media from Firebase", e)
        }
    }
}