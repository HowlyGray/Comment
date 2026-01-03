package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.MediaDao
import com.memoryshare.app.data.local.dao.SharedSpaceDao
import com.memoryshare.app.data.model.Media
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.SharedSpace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class SharedSpaceRepository(
    private val sharedSpaceDao: SharedSpaceDao,
    private val mediaDao: MediaDao
) {

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
        sharedSpaceDao.insertSharedSpace(space)
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
        mediaDao.insertMedia(media)

        // Mettre à jour le nombre de médias et la dernière activité de l'espace
        val space = sharedSpaceDao.getSharedSpaceById(spaceId).firstOrNull()
        space?.let {
            sharedSpaceDao.updateSharedSpace(
                it.copy(
                    mediaCount = it.mediaCount + 1,
                    lastActivityAt = System.currentTimeMillis()
                )
            )
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
        mediaDao.deleteMedia(media)

        // Mettre à jour le nombre de médias de l'espace
        val space = sharedSpaceDao.getSharedSpaceById(media.spaceId).firstOrNull()
        space?.let {
            sharedSpaceDao.updateSharedSpace(
                it.copy(mediaCount = (it.mediaCount - 1).coerceAtLeast(0))
            )
        }
    }

    suspend fun deleteSharedSpace(space: SharedSpace) {
        mediaDao.deleteMediaBySpace(space.id)
        sharedSpaceDao.deleteSharedSpace(space)
    }
}
