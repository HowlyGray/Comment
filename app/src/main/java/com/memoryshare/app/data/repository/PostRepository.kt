package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.CommentDao
import com.memoryshare.app.data.local.dao.PostDao
import com.memoryshare.app.data.model.Comment
import com.memoryshare.app.data.model.Post
import com.memoryshare.app.data.model.PostMediaType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PostRepository(
    private val postDao: PostDao,
    private val commentDao: CommentDao
) {

    fun getAllPosts(): Flow<List<Post>> = postDao.getAllPosts()

    fun getPostsByUser(userId: String): Flow<List<Post>> = postDao.getPostsByUser(userId)

    fun getPostById(postId: String): Flow<Post?> = postDao.getPostById(postId)

    fun getCommentsByPost(postId: String): Flow<List<Comment>> = commentDao.getCommentsByPost(postId)

    suspend fun createPost(
        authorId: String,
        mediaUrls: List<String>,
        mediaType: PostMediaType,
        caption: String? = null,
        thumbnailUrl: String? = null
    ): Post {
        val post = Post(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            caption = caption,
            mediaUrls = mediaUrls,
            mediaType = mediaType,
            thumbnailUrl = thumbnailUrl
        )
        postDao.insertPost(post)
        return post
    }

    suspend fun toggleLike(post: Post) {
        val updatedPost = if (post.isLikedByCurrentUser) {
            post.copy(
                likeCount = (post.likeCount - 1).coerceAtLeast(0),
                isLikedByCurrentUser = false
            )
        } else {
            post.copy(
                likeCount = post.likeCount + 1,
                isLikedByCurrentUser = true
            )
        }
        postDao.updatePost(updatedPost)
    }

    suspend fun addComment(postId: String, authorId: String, content: String): Comment {
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            postId = postId,
            authorId = authorId,
            content = content
        )
        commentDao.insertComment(comment)

        // Mettre à jour le nombre de commentaires
        postDao.getPostById(postId).collect { post ->
            post?.let {
                postDao.updatePost(it.copy(commentCount = it.commentCount + 1))
            }
        }

        return comment
    }

    suspend fun deletePost(post: Post) {
        commentDao.deleteCommentsByPost(post.id)
        postDao.deletePost(post)
    }

    suspend fun deleteComment(comment: Comment) {
        commentDao.deleteComment(comment)

        // Mettre à jour le nombre de commentaires
        postDao.getPostById(comment.postId).collect { post ->
            post?.let {
                postDao.updatePost(it.copy(commentCount = (it.commentCount - 1).coerceAtLeast(0)))
            }
        }
    }
}
