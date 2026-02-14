package com.memoryshare.app.data.repository

import android.util.Log
import com.memoryshare.app.data.local.dao.CommentDao
import com.memoryshare.app.data.local.dao.PostDao
import com.memoryshare.app.data.local.dao.UserFollowDao
import com.memoryshare.app.data.model.Comment
import com.memoryshare.app.data.model.Post
import com.memoryshare.app.data.model.PostMediaType
import com.memoryshare.app.utils.FirebaseManager
import com.memoryshare.app.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val userFollowDao: UserFollowDao? = null,
    private val context: android.content.Context? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore = FirebaseManager.firestore

    companion object {
        private const val TAG = "PostRepository"
    }

    fun getAllPosts(): Flow<List<Post>> = postDao.getAllPosts()

    fun getPublicPosts(): Flow<List<Post>> = postDao.getPublicPosts()

    fun getFeedPosts(currentUserId: String): Flow<List<Post>> = postDao.getFeedPosts(currentUserId)

    fun getPostsByUser(userId: String): Flow<List<Post>> = postDao.getPostsByUser(userId)

    fun getFollowingPosts(userId: String): Flow<List<Post>> = postDao.getFollowingPosts(userId)

    fun getPostById(postId: String): Flow<Post?> = postDao.getPostById(postId)

    fun getCommentsByPost(postId: String): Flow<List<Comment>> = commentDao.getCommentsByPost(postId)

    suspend fun createPost(
        authorId: String,
        mediaUrls: List<String>,
        mediaType: PostMediaType,
        caption: String? = null,
        thumbnailUrl: String? = null,
        visibility: com.memoryshare.app.data.model.PostVisibility = com.memoryshare.app.data.model.PostVisibility.PUBLIC
    ): Post {
        val post = Post(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            caption = caption,
            mediaUrls = mediaUrls,
            mediaType = mediaType,
            thumbnailUrl = thumbnailUrl,
            visibility = visibility
        )
        // Sauvegarder localement
        postDao.insertPost(post)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.POSTS,
                documentId = post.id,
                data = post
            ).onSuccess {
                Log.d(TAG, "Post synced to Firebase: ${post.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync post to Firebase: ${post.id}", e)
            }
        }

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
        // Mettre à jour localement
        postDao.updatePost(updatedPost)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.POSTS,
                documentId = updatedPost.id,
                data = updatedPost
            ).onSuccess {
                Log.d(TAG, "Post like synced to Firebase: ${updatedPost.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync post like to Firebase: ${updatedPost.id}", e)
            }
        }
    }

    suspend fun addComment(postId: String, authorId: String, content: String): Comment {
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            postId = postId,
            authorId = authorId,
            content = content
        )
        // Sauvegarder localement
        commentDao.insertComment(comment)

        // Mettre à jour le nombre de commentaires localement
        val post = postDao.getPostById(postId).firstOrNull()
        post?.let {
            val updatedPost = it.copy(commentCount = it.commentCount + 1)
            postDao.updatePost(updatedPost)

            // Synchroniser le commentaire et le post avec Firebase
            scope.launch {
                // Sauvegarder le commentaire
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.COMMENTS,
                    documentId = comment.id,
                    data = comment
                ).onSuccess {
                    Log.d(TAG, "Comment synced to Firebase: ${comment.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to sync comment to Firebase: ${comment.id}", e)
                }

                // Mettre à jour le post
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.POSTS,
                    documentId = updatedPost.id,
                    data = updatedPost
                ).onSuccess {
                    Log.d(TAG, "Post comment count synced to Firebase: ${updatedPost.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to sync post comment count to Firebase: ${updatedPost.id}", e)
                }
            }
        }

        return comment
    }

    suspend fun updatePost(post: Post) {
        // Mettre à jour localement
        postDao.updatePost(post)

        // Synchroniser avec Firebase
        scope.launch {
            FirebaseManager.saveDocument(
                collection = FirebaseManager.Collections.POSTS,
                documentId = post.id,
                data = post
            ).onSuccess {
                Log.d(TAG, "Post updated in Firebase: ${post.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to update post in Firebase: ${post.id}", e)
            }
        }
    }

    suspend fun deletePost(post: Post) {
        // Supprimer localement
        commentDao.deleteCommentsByPost(post.id)
        postDao.deletePost(post)

        // Supprimer de Firebase
        scope.launch {
            // Supprimer le post
            FirebaseManager.deleteDocument(
                collection = FirebaseManager.Collections.POSTS,
                documentId = post.id
            ).onSuccess {
                Log.d(TAG, "Post deleted from Firebase: ${post.id}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete post from Firebase: ${post.id}", e)
            }

            // Supprimer les commentaires associés
            // Note: Dans une vraie app, on ferait une query pour récupérer tous les commentaires du post
            // Pour simplifier ici, on suppose que les commentaires seront nettoyés via Cloud Functions
        }
    }

    suspend fun deleteComment(comment: Comment) {
        // Supprimer localement
        commentDao.deleteComment(comment)

        // Mettre à jour le nombre de commentaires localement
        val post = postDao.getPostById(comment.postId).firstOrNull()
        post?.let {
            val updatedPost = it.copy(commentCount = (it.commentCount - 1).coerceAtLeast(0))
            postDao.updatePost(updatedPost)

            // Synchroniser avec Firebase
            scope.launch {
                // Supprimer le commentaire
                FirebaseManager.deleteDocument(
                    collection = FirebaseManager.Collections.COMMENTS,
                    documentId = comment.id
                ).onSuccess {
                    Log.d(TAG, "Comment deleted from Firebase: ${comment.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to delete comment from Firebase: ${comment.id}", e)
                }

                // Mettre à jour le post
                FirebaseManager.saveDocument(
                    collection = FirebaseManager.Collections.POSTS,
                    documentId = updatedPost.id,
                    data = updatedPost
                ).onSuccess {
                    Log.d(TAG, "Post comment count synced to Firebase after deletion: ${updatedPost.id}")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to sync post comment count to Firebase after deletion: ${updatedPost.id}", e)
                }
            }
        }
    }

    /**
     * Synchronise les posts publics depuis Firebase vers la base de données locale
     * Récupère tous les posts publics pour le fil d'actualité
     */
    suspend fun syncPostsFromFirebase() {
        try {
            // Requête simple sans orderBy pour éviter l'index composite Firestore
            // Le tri se fait côté local via Room (ORDER BY timestamp DESC)
            val querySnapshot = firestore.collection(FirebaseManager.Collections.POSTS)
                .whereEqualTo("visibility", "PUBLIC")
                .limit(100)
                .get()
                .await()

            val posts = querySnapshot.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    Post(
                        id = doc.getString("id") ?: doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        caption = doc.getString("caption"),
                        mediaUrls = doc.get("mediaUrls") as? List<String> ?: emptyList(),
                        mediaType = PostMediaType.valueOf(doc.getString("mediaType") ?: "IMAGE"),
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        likeCount = doc.getLong("likeCount")?.toInt() ?: 0,
                        commentCount = doc.getLong("commentCount")?.toInt() ?: 0,
                        visibility = try {
                            com.memoryshare.app.data.model.PostVisibility.valueOf(
                                doc.getString("visibility") ?: "PUBLIC"
                            )
                        } catch (e: Exception) {
                            com.memoryshare.app.data.model.PostVisibility.PUBLIC
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse post: ${doc.id}", e)
                    null
                }
            }

            if (posts.isNotEmpty()) {
                // Détecter les nouveaux posts d'utilisateurs suivis pour les notifications
                if (context != null && userFollowDao != null) {
                    try {
                        val currentUserId = FirebaseManager.getCurrentUserId()
                        if (currentUserId != null) {
                            val existingPostIds = postDao.getAllPostIds().toSet()
                            val newPosts = posts.filter { it.id !in existingPostIds }
                            if (newPosts.isNotEmpty()) {
                                val followingIds = userFollowDao.getFollowingIdsSync(currentUserId).toSet()
                                val newPostsFromFollowed = newPosts.filter { it.authorId in followingIds && it.authorId != currentUserId }
                                for (post in newPostsFromFollowed) {
                                    NotificationHelper.notifyNewPost(
                                        context = context,
                                        authorName = post.authorId,
                                        postPreview = post.caption
                                    )
                                    Log.d(TAG, "Notification: new post from followed user ${post.authorId}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to check for new post notifications", e)
                    }
                }
                postDao.insertPosts(posts)
            }
            Log.d(TAG, "Synced ${posts.size} public posts from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync posts from Firebase", e)
        }
    }

    /**
     * Synchronise les commentaires depuis Firebase vers la base de données locale
     */
    suspend fun syncCommentsFromFirebase() {
        FirebaseManager.getCollection(
            collection = FirebaseManager.Collections.COMMENTS,
            clazz = Comment::class.java
        ).onSuccess { comments ->
            comments.forEach { comment ->
                commentDao.insertComment(comment)
            }
            Log.d(TAG, "Synced ${comments.size} comments from Firebase")
        }.onFailure { e ->
            Log.e(TAG, "Failed to sync comments from Firebase", e)
        }
    }
}
