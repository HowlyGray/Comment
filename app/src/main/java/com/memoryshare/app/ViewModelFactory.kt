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

/**
 * Type-safe factory for creating ViewModels with their required dependencies.
 */
class UserViewModelFactory(
    private val repository: UserRepository,
    private val preferencesManager: PreferencesManager,
    private val messageRepository: MessageRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            return UserViewModel(repository, preferencesManager, messageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class MessageViewModelFactory(
    private val repository: MessageRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            val mediaSyncManager = createMediaSyncManager(context)
            return MessageViewModel(repository, mediaSyncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class PostViewModelFactory(
    private val repository: PostRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
            val mediaSyncManager = createMediaSyncManager(context)
            return PostViewModel(repository, mediaSyncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class SharedSpaceViewModelFactory(
    private val repository: SharedSpaceRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedSpaceViewModel::class.java)) {
            val mediaSyncManager = createMediaSyncManager(context)
            return SharedSpaceViewModel(repository, mediaSyncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class StoryViewModelFactory(
    private val repository: StoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryViewModel::class.java)) {
            return StoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class CallViewModelFactory(
    private val repository: CallRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            return CallViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Backward-compatible factory that delegates to the appropriate typed factory.
 * Retained for compatibility with existing code that creates ViewModelFactory directly.
 */
class ViewModelFactory(
    private val repository: Any,
    private val preferencesManager: PreferencesManager? = null,
    private val context: Context? = null,
    private val messageRepository: MessageRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(
                    repository as UserRepository,
                    preferencesManager ?: throw IllegalArgumentException("PreferencesManager required for UserViewModel"),
                    messageRepository
                ) as T
            }
            modelClass.isAssignableFrom(MessageViewModel::class.java) -> {
                val mediaSyncManager = createMediaSyncManager(context)
                MessageViewModel(repository as MessageRepository, mediaSyncManager) as T
            }
            modelClass.isAssignableFrom(PostViewModel::class.java) -> {
                val mediaSyncManager = createMediaSyncManager(context)
                PostViewModel(repository as PostRepository, mediaSyncManager) as T
            }
            modelClass.isAssignableFrom(SharedSpaceViewModel::class.java) -> {
                val mediaSyncManager = createMediaSyncManager(context)
                SharedSpaceViewModel(repository as SharedSpaceRepository, mediaSyncManager) as T
            }
            modelClass.isAssignableFrom(StoryViewModel::class.java) -> {
                StoryViewModel(repository as StoryRepository) as T
            }
            modelClass.isAssignableFrom(CallViewModel::class.java) -> {
                CallViewModel(repository as CallRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private fun createMediaSyncManager(context: Context?): MediaSyncManager? {
    return if (context != null) {
        val database = AppDatabase.getDatabase(context)
        val compressionManager = MediaCompressionManager(context)
        val storageManager = FirebaseStorageManager()
        MediaSyncManager(context, database.mediaCacheDao(), compressionManager, storageManager)
    } else null
}
