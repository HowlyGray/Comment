package com.memoryshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.memoryshare.app.data.local.AppDatabase
import com.memoryshare.app.data.local.PreferencesManager
import com.memoryshare.app.data.repository.MessageRepository
import com.memoryshare.app.data.repository.PostRepository
import com.memoryshare.app.data.repository.SharedSpaceRepository
import com.memoryshare.app.data.repository.UserRepository
import com.memoryshare.app.ui.navigation.AppNavigation
import com.memoryshare.app.ui.theme.MemoryShareTheme
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.PreferencesViewModel
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialiser la base de données
        val database = AppDatabase.getDatabase(applicationContext)

        // Initialiser le gestionnaire de préférences
        val preferencesManager = PreferencesManager(applicationContext)

        // Initialiser les repositories
        val userRepository = UserRepository(
            database.userDao(),
            database.userFollowDao()
        )
        val messageRepository = MessageRepository(
            database.messageDao(),
            database.conversationDao(),
            database.messageReactionDao()
        )
        val postRepository = PostRepository(
            database.postDao(),
            database.commentDao()
        )
        val spaceRepository = SharedSpaceRepository(
            database.sharedSpaceDao(),
            database.mediaDao(),
            database.sharedSpacePermissionDao()
        )

        setContent {
            MemoryShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MemoryShareApp(
                        userRepository = userRepository,
                        messageRepository = messageRepository,
                        postRepository = postRepository,
                        spaceRepository = spaceRepository,
                        preferencesManager = preferencesManager
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryShareApp(
    userRepository: UserRepository,
    messageRepository: MessageRepository,
    postRepository: PostRepository,
    spaceRepository: SharedSpaceRepository,
    preferencesManager: PreferencesManager
) {
    val navController = rememberNavController()

    // Créer les ViewModels
    val userViewModel = viewModel<UserViewModel>(
        factory = ViewModelFactory(userRepository, preferencesManager)
    )
    val messageViewModel = viewModel<MessageViewModel>(
        factory = ViewModelFactory(messageRepository)
    )
    val postViewModel = viewModel<PostViewModel>(
        factory = ViewModelFactory(postRepository)
    )
    val spaceViewModel = viewModel<SharedSpaceViewModel>(
        factory = ViewModelFactory(spaceRepository)
    )
    val preferencesViewModel = PreferencesViewModel(preferencesManager)

    AppNavigation(
        navController = navController,
        userViewModel = userViewModel,
        messageViewModel = messageViewModel,
        postViewModel = postViewModel,
        spaceViewModel = spaceViewModel,
        preferencesViewModel = preferencesViewModel
    )
}
