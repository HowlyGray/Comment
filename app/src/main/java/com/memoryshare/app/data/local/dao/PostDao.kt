package com.memoryshare.app.data.local.dao

import androidx.room.*
import com.memoryshare.app.data.model.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE visibility = 'PUBLIC' ORDER BY timestamp DESC")
    fun getPublicPosts(): Flow<List<Post>>

    @Query("""
        SELECT * FROM posts
        WHERE visibility = 'PUBLIC' OR authorId = :currentUserId
        ORDER BY timestamp DESC
    """)
    fun getFeedPosts(currentUserId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE authorId = :userId ORDER BY timestamp DESC")
    fun getPostsByUser(userId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostById(postId: String): Flow<Post?>

    @Query("""
        SELECT posts.* FROM posts
        INNER JOIN user_follows ON posts.authorId = user_follows.followingId
        WHERE user_follows.followerId = :userId
        AND (posts.visibility = 'PUBLIC' OR posts.visibility = 'FOLLOWERS' OR posts.visibility = 'FRIENDS_AND_FOLLOWERS')
        ORDER BY posts.timestamp DESC
    """)
    fun getFollowingPosts(userId: String): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Update
    suspend fun updatePost(post: Post)

    @Delete
    suspend fun deletePost(post: Post)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}
