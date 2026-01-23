package com.memoryshare.app.data.repository

import android.util.Log
import com.memoryshare.app.data.local.dao.StoryDao
import com.memoryshare.app.data.model.Story
import com.memoryshare.app.data.model.StoryMediaType
import com.memoryshare.app.utils.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class StoryRepository(private val storyDao: StoryDao) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "StoryRepository"
        private const val STORY_DURATION_MS = 24 * 60 * 60 * 1000L // 24 heures
    }

    fun getActiveStories(): Flow<List<Story>> = storyDao.getActiveStories()

    fun getStoriesByUser(userId: String): Flow<List<Story>> = storyDao.getStoriesByUser(userId)

    fun getStoryById(storyId: String): Flow<Story?> = storyDao.getStoryById(storyId)

    fun getAuthorsWithActiveStories(): Flow<List<String>> = storyDao.getAuthorsWithActiveStories()

    suspend fun createStory(
        authorId: String,
        mediaUrl: String,
        mediaType: StoryMediaType,
        thumbnailUrl: String? = null,
        caption: String? = null,
        duration: Long? = null
    ): Story {
        val story = Story(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            thumbnailUrl = thumbnailUrl,
            caption = caption,
            duration = duration
        )
        // Sauvegarder localement
        storyDao.insertStory(story)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.STORIES,
                documentId = story.id,
                data = story
            ).onSuccess {
                Log.d(TAG, "Story synced to Firebase: ${story.id}")
                // Planifier la suppression automatique après 24h
                scheduleStoryDeletion(story)
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync story to Firebase: ${story.id}", e)
            }
        }

        return story
    }

    suspend fun markAsViewed(story: Story, userId: String) {
        if (!story.viewedBy.contains(userId)) {
            val updatedStory = story.copy(
                viewedBy = story.viewedBy + userId,
                viewCount = story.viewCount + 1
            )
            // Mettre à jour localement
            storyDao.updateStory(updatedStory)

            // Synchroniser avec Firebase
            scope.launch {
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.STORIES,
                    documentId = updatedStory.id,
                    data = updatedStory
                ).onSuccess {
                    Log.d(TAG, "Story view synced to Firebase: ${updatedStory.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to sync story view to Firebase: ${updatedStory.id}", e)
                }
            }
        }
    }

    suspend fun deleteStory(story: Story) {
        // Supprimer localement
        storyDao.deleteStory(story)

        // Supprimer de Firebase
        scope.launch {
            FirebaseManager.deleteDocument(
                collection = FirebaseManager.Collections.STORIES,
                documentId = story.id
            ).onSuccess {
                Log.d(TAG, "Story deleted from Firebase: ${story.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete story from Firebase: ${story.id}", e)
            }
        }
    }

    suspend fun deleteExpiredStories() {
        // Supprimer localement
        storyDao.deleteExpiredStories()

        // Synchroniser avec Firebase - récupérer et supprimer les stories expirées
        scope.launch {
            FirebaseManager.getCollection(
                collection = FirebaseManager.Collections.STORIES,
                clazz = Story::class.java
            ).onSuccess { stories ->
                val currentTime = System.currentTimeMillis()
                val expiredStories = stories.filter { story ->
                    (currentTime - story.createdAt) > STORY_DURATION_MS
                }

                expiredStories.forEach { story ->
                    FirebaseManager.deleteDocument(
                        collection = FirebaseManager.Collections.STORIES,
                        documentId = story.id
                    ).onSuccess {
                        Log.d(TAG, "Expired story deleted from Firebase: ${story.id}")
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to delete expired story from Firebase: ${story.id}", e)
                    }
                }

                if (expiredStories.isNotEmpty()) {
                    Log.d(TAG, "Deleted ${expiredStories.size} expired stories from Firebase")
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to fetch stories from Firebase for cleanup", e)
            }
        }
    }

    /**
     * Planifie la suppression automatique d'une story après 24h
     */
    private fun scheduleStoryDeletion(story: Story) {
        scope.launch {
            val expiryTime = story.createdAt + STORY_DURATION_MS
            val currentTime = System.currentTimeMillis()
            val delay = expiryTime - currentTime

            if (delay > 0) {
                kotlinx.coroutines.delay(delay)
                // Supprimer de Firebase après 24h
                FirebaseManager.deleteDocument(
                    collection = FirebaseManager.Collections.STORIES,
                    documentId = story.id
                ).onSuccess {
                    Log.d(TAG, "Story auto-deleted from Firebase after 24h: ${story.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to auto-delete story from Firebase: ${story.id}", e)
                }
            }
        }
    }

    /**
     * Synchronise les stories depuis Firebase vers la base de données locale
     */
    suspend fun syncStoriesFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.STORIES,
            clazz = Story::class.java
        ).onSuccess { stories ->
            stories.forEach { story ->
                storyDao.insertStory(story)
            }
            Log.d(TAG, "Synced ${stories.size} stories from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync stories from Firebase", e)
        }
    }
}
