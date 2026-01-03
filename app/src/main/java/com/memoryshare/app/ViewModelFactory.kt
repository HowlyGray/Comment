package com.memoryshare.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.memoryshare.app.data.local.PreferencesManager
import com.memoryshare.app.data.repository.MessageRepository
import com.memoryshare.app.data.repository.PostRepository
import com.memoryshare.app.data.repository.SharedSpaceRepository
import com.memoryshare.app.data.repository.UserRepository
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel

class ViewModelFactory(
    private val repository: Any,
    private val preferencesManager: PreferencesManager? = null
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
                MessageViewModel(repository as MessageRepository) as T
            }
            modelClass.isAssignableFrom(PostViewModel::class.java) -> {
                PostViewModel(repository as PostRepository) as T
            }
            modelClass.isAssignableFrom(SharedSpaceViewModel::class.java) -> {
                SharedSpaceViewModel(repository as SharedSpaceRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
