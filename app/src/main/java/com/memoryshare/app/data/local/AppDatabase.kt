package com.memoryshare.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.memoryshare.app.data.local.dao.*
import com.memoryshare.app.data.model.*

@Database(
    entities = [
        User::class,
        UserFollow::class,
        Conversation::class,
        Message::class,
        MessageReaction::class,
        Post::class,
        Story::class,
        Reel::class,
        SharedSpace::class,
        Media::class,
        MediaComment::class,
        Comment::class,
        SharedSpacePermission::class,
        Call::class,
        MediaCache::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userFollowDao(): UserFollowDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun reelDao(): ReelDao
    abstract fun sharedSpaceDao(): SharedSpaceDao
    abstract fun mediaDao(): MediaDao
    abstract fun mediaCommentDao(): MediaCommentDao
    abstract fun commentDao(): CommentDao
    abstract fun sharedSpacePermissionDao(): SharedSpacePermissionDao
    abstract fun callDao(): CallDao
    abstract fun mediaCacheDao(): MediaCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memoryshare_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
