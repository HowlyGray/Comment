package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.CommentDao
import com.memoryshare.app.data.local.dao.ReelDao
import com.memoryshare.app.data.model.Comment
import com.memoryshare.app.data.model.Reel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class ReelRepository(
    private val reelDao: ReelDao,
    private val commentDao: CommentDao
) {

    fun getAllReels(): Flow<List<Reel>> = reelDao.getAllReels()

    fun getReelsByUser(userId: String): Flow<List<Reel>> = reelDao.getReelsByUser(userId)

    fun getReelById(reelId: String): Flow<Reel?> = reelDao.getReelById(reelId)

    suspend fun createReel(
        authorId: String,
        videoUrl: String,
        thumbnailUrl: String? = null,
        caption: String? = null,
        duration: Long,
        audioUrl: String? = null,
        audioName: String? = null
    ): Reel {
        val reel = Reel(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            caption = caption,
            duration = duration,
            audioUrl = audioUrl,
            audioName = audioName
        )
        reelDao.insertReel(reel)
        return reel
    }

    suspend fun toggleLike(reel: Reel) {
        val updatedReel = if (reel.isLikedByCurrentUser) {
            reel.copy(
                likeCount = (reel.likeCount - 1).coerceAtLeast(0),
                isLikedByCurrentUser = false
            )
        } else {
            reel.copy(
                likeCount = reel.likeCount + 1,
                isLikedByCurrentUser = true
            )
        }
        reelDao.updateReel(updatedReel)
    }

    suspend fun incrementViewCount(reel: Reel) {
        val updatedReel = reel.copy(viewCount = reel.viewCount + 1)
        reelDao.updateReel(updatedReel)
    }

    suspend fun addComment(reelId: String, authorId: String, content: String): Comment {
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            postId = reelId, // On réutilise le champ postId pour les reels
            authorId = authorId,
            content = content
        )
        commentDao.insertComment(comment)

        // Mettre à jour le nombre de commentaires
        val reel = reelDao.getReelById(reelId).firstOrNull()
        reel?.let {
            reelDao.updateReel(it.copy(commentCount = it.commentCount + 1))
        }

        return comment
    }

    suspend fun deleteReel(reel: Reel) {
        reelDao.deleteReel(reel)
    }
}
