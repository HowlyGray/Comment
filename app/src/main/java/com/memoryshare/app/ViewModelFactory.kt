package com.memoryshare.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.memoryshare.app.data.local.AppDatabase
import com.memoryshare.app.data.local.PreferencesManager
import com.memoryshare.app.data.repository.CallRepository
import com.memoryshare.app.data.repository.MessageRepository
import com.memoryshare.app.data.repository.PostRepository
import com.memoryshare.app.data.repository.SharedSpaceRepository
import com.memoryshare.app.data.repository.StoryRepository
import com.memoryshare.app.data.repository.UserRepository
import com.memoryshare.app.ui.viewmodel.CallViewModel
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.StoryViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import com.memoryshare.app.utils.FirebaseStorageManager
import com.memoryshare.app.utils.MediaCompressionManager
import com.memoryshare.app.utils.MediaSyncManager

class ViewModelFactory(
    private val repository: Any,
    private val preferencesManager: PreferencesManager? = null,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(
                    repository as UserRepository,
                    preferencesManager ?: throw IllegalArgumentException("PreferencesManager required for UserViewModel")
                ) as T
            }
            modelClass.isAssignableFrom(MessageViewModel::class.java) -> {
                val mediaSyncManager = if (context != null) {
                    val database = AppDatabase.getDatabase(context)
                    val compressionManager = MediaCompressionManager(context)
                    val storageManager = FirebaseStorageManager()
                    MediaSyncManager(context, database.mediaCacheDao(), compressionManager, storageManager)
                } else null
                MessageViewModel(repository as MessageRepository, mediaSyncManager) as T
            }
            modelClass.isAssignableFrom(PostViewModel::class.java) -> {
                val mediaSyncManager = if (context != null) {
                    val database = AppDatabase.getDatabase(context)
                    val compressionManager = MediaCompressionManager(context)
                    val storageManager = FirebaseStorageManager()
                    MediaSyncManager(context, database.mediaCacheDao(), compressionManager, storageManager)
                } else null
                PostViewModel(repository as PostRepository, mediaSyncManager) as T
            }
            modelClass.isAssignableFrom(SharedSpaceViewModel::class.java) -> {
                val mediaSyncManager = if (context != null) {
                    val database = AppDatabase.getDatabase(context)
                    val compressionManager = MediaCompressionManager(context)
                    val storageManager = FirebaseStorageManager()
                    MediaSyncManager(context, database.mediaCacheDao(), compressionManager, storageManager)
                } else null
                SharedSpaceViewModel(repository as SharedSpaceRepository, mediaSyncManager) as T
            }
            modelClass.isAssignableFrom(StoryViewModel::class.java) -> {
                StoryViewModel(repository as StoryRepository) as T
            }
            modelClass.isAssignableFrom(CallViewModel::class.java) -> {
                CallViewModel(repository as CallRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
