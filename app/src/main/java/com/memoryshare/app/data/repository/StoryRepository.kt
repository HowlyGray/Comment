package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.StoryDao
import com.memoryshare.app.data.model.Story
import com.memoryshare.app.data.model.StoryMediaType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StoryRepository(private val storyDao: StoryDao) {

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
        storyDao.insertStory(story)
        return story
    }

    suspend fun markAsViewed(story: Story, userId: String) {
        if (!story.viewedBy.contains(userId)) {
            val updatedStory = story.copy(
                viewedBy = story.viewedBy + userId,
                viewCount = story.viewCount + 1
            )
            storyDao.updateStory(updatedStory)
        }
    }

    suspend fun deleteStory(story: Story) {
        storyDao.deleteStory(story)
    }

    suspend fun deleteExpiredStories() {
        storyDao.deleteExpiredStories()
    }
}
