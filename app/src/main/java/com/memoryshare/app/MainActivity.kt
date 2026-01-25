package com.memoryshare.app

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.memoryshare.app.data.local.AppDatabase
import com.memoryshare.app.data.local.PreferencesManager
import com.memoryshare.app.data.repository.CallRepository
import com.memoryshare.app.data.repository.MessageRepository
import com.memoryshare.app.data.repository.PostRepository
import com.memoryshare.app.data.repository.SharedSpaceRepository
import com.memoryshare.app.data.repository.StoryRepository
import com.memoryshare.app.data.repository.UserRepository
import com.memoryshare.app.ui.navigation.AppNavigation
import com.memoryshare.app.ui.theme.MemoryShareTheme
import com.memoryshare.app.ui.viewmodel.CallViewModel
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.PreferencesViewModel
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.StoryViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import com.memoryshare.app.utils.MediaSyncScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialiser Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase initialized successfully")

            // Configurer Firestore pour la persistence hors ligne
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            Log.d("Firestore", "Firestore configured with offline persistence")
        } catch (e: Exception) {
            Log.e("Firebase", "Error initializing Firebase", e)
        }

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
        val storyRepository = StoryRepository(
            database.storyDao()
        )
        val callRepository = CallRepository(
            database.callDao()
        )

        // Initialiser la synchronisation périodique des médias
        try {
            MediaSyncScheduler.startPeriodicSync(
                context = applicationContext,
                intervalHours = 6,        // Sync toutes les 6 heures
                cleanupDays = 30,         // Nettoyer les médias de plus de 30 jours
                requiresCharging = false, // Pas besoin de charge
                requiresWifi = true       // Uniquement en WiFi pour économiser les données
            )
            Log.d("MediaSync", "Periodic media sync scheduled successfully")
        } catch (e: Exception) {
            Log.e("MediaSync", "Error scheduling media sync", e)
        }

        setContent {
            MemoryShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MemoryShareApp(
                        context = applicationContext,
                        userRepository = userRepository,
                        messageRepository = messageRepository,
                        postRepository = postRepository,
                        spaceRepository = spaceRepository,
                        storyRepository = storyRepository,
                        callRepository = callRepository,
                        preferencesManager = preferencesManager
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryShareApp(
    context: Context,
    userRepository: UserRepository,
    messageRepository: MessageRepository,
    postRepository: PostRepository,
    spaceRepository: SharedSpaceRepository,
    storyRepository: StoryRepository,
    callRepository: CallRepository,
    preferencesManager: PreferencesManager
) {
    val navController = rememberNavController()

    // Créer les ViewModels
    val userViewModel = viewModel<UserViewModel>(
        factory = ViewModelFactory(userRepository, preferencesManager)
    )
    val messageViewModel = viewModel<MessageViewModel>(
        factory = ViewModelFactory(messageRepository, context = context)
    )
    val postViewModel = viewModel<PostViewModel>(
        factory = ViewModelFactory(postRepository, context = context)
    )
    val spaceViewModel = viewModel<SharedSpaceViewModel>(
        factory = ViewModelFactory(spaceRepository, context = context)
    )
    val storyViewModel = viewModel<StoryViewModel>(
        factory = ViewModelFactory(storyRepository)
    )
    val callViewModel = viewModel<CallViewModel>(
        factory = ViewModelFactory(callRepository)
    )
    val preferencesViewModel = PreferencesViewModel(preferencesManager)

    AppNavigation(
        navController = navController,
        userViewModel = userViewModel,
        messageViewModel = messageViewModel,
        postViewModel = postViewModel,
        spaceViewModel = spaceViewModel,
        storyViewModel = storyViewModel,
        preferencesViewModel = preferencesViewModel,
        callViewModel = callViewModel
    )
}
