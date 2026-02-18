package com.memoryshare.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.memoryshare.app.MemoryShareApplication
import com.memoryshare.app.ui.screens.*
import com.memoryshare.app.ui.viewmodel.CallViewModel
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.PreferencesViewModel
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.StoryViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController,
    userViewModel: UserViewModel,
    messageViewModel: MessageViewModel,
    postViewModel: PostViewModel,
    spaceViewModel: SharedSpaceViewModel,
    storyViewModel: StoryViewModel,
    preferencesViewModel: PreferencesViewModel,
    callViewModel: CallViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext

    // Toujours démarrer sur Login, puis naviguer automatiquement si un utilisateur est restauré
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // Initialize RealtimeSyncManager for message notifications
            // This must happen at the navigation level (not per-screen) because
            // Firebase.auth.currentUser may be null on guest devices
            currentUser?.id?.let { userId ->
                (appContext as? MemoryShareApplication)?.ensureRealtimeSyncInitialized(userId)
            }

            // Naviguer vers Messages si un utilisateur est restauré et qu'on est sur Login
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == Screen.Login.route) {
                navController.navigate(Screen.Messages.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { user ->
                    userViewModel.setCurrentUser(user)
                    navController.navigate(Screen.Messages.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                userViewModel = userViewModel
            )
        }

        composable(Screen.Messages.route) {
            MessagesScreen(
                viewModel = messageViewModel,
                userViewModel = userViewModel,
                preferencesViewModel = preferencesViewModel,
                currentUser = currentUser,
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.MessageDetail.createRoute(conversationId))
                },
                onNewConversation = {
                    navController.navigate(Screen.SelectContact.route)
                },
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route)
                },
                onNavigateToMemories = {
                    navController.navigate(Screen.Memories.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToArchived = {
                    navController.navigate(Screen.ArchivedConversations.route)
                },
                onNavigateToAllStarred = {
                    navController.navigate(Screen.AllStarredMessages.route)
                }
            )
        }

        composable(
            route = Screen.SelectContact.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            SelectContactScreen(
                userViewModel = userViewModel,
                currentUser = currentUser,
                onContactSelected = { selectedUser ->
                    // Trouver ou créer une conversation avec le contact sélectionné
                    currentUser?.let { user ->
                        coroutineScope.launch {
                            val conversation = messageViewModel.findOrCreateConversation(
                                participantIds = listOf(user.id, selectedUser.id),
                                name = null,
                                isGroup = false
                            )
                            navController.navigate(Screen.MessageDetail.createRoute(conversation.id)) {
                                popUpTo(Screen.Messages.route) { inclusive = false }
                            }
                        }
                    }
                },
                onCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                },
                onAddNewContact = {
                    navController.navigate(Screen.AddContact.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateGroup.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            CreateGroupScreen(
                userViewModel = userViewModel,
                currentUser = currentUser,
                onCreateGroup = { selectedUsers, groupName ->
                    // Créer un groupe avec les utilisateurs sélectionnés
                    currentUser?.let { user ->
                        coroutineScope.launch {
                            val participantIds = selectedUsers.map { it.id } + user.id
                            val conversation = messageViewModel.findOrCreateConversation(
                                participantIds = participantIds,
                                name = groupName,
                                isGroup = true
                            )
                            navController.navigate(Screen.MessageDetail.createRoute(conversation.id)) {
                                popUpTo(Screen.Messages.route) { inclusive = false }
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddContact.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            AddContactScreen(
                userViewModel = userViewModel,
                onContactAdded = {
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MessageDetail.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            MessageDetailScreen(
                conversationId = conversationId,
                viewModel = messageViewModel,
                userViewModel = userViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onNavigateToContactDetail = { userId ->
                    navController.navigate(Screen.ContactDetail.createRoute(userId))
                },
                onNavigateToVideoCall = { userId ->
                    navController.navigate(Screen.VideoCall.createRoute(userId))
                },
                onNavigateToVoiceCall = { userId ->
                    navController.navigate(Screen.Call.createRoute(userId))
                },
                onNavigateToForward = { navController.navigate(Screen.ForwardMessages.route) },
                onNavigateToMediaViewer = { messageId ->
                    navController.navigate(Screen.MessageMediaViewer.createRoute(messageId))
                }
            )
        }

        composable(Screen.Feed.route) {
            FeedScreen(
                viewModel = postViewModel,
                userViewModel = userViewModel,
                preferencesViewModel = preferencesViewModel,
                currentUser = currentUser,
                onPostClick = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onCreatePost = {
                    navController.navigate(Screen.CreatePost.route)
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToMemories = {
                    navController.navigate(Screen.Memories.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onMediaClick = { postId ->
                    navController.navigate(Screen.MediaViewer.createRoute(postId))
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.ContactDetail.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.CreatePost.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            CreatePostScreen(
                viewModel = postViewModel,
                storyViewModel = storyViewModel,
                preferencesViewModel = preferencesViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onPostCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            PostDetailScreen(
                postId = postId,
                viewModel = postViewModel,
                userViewModel = userViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.ContactDetail.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.MediaViewer.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            val posts by postViewModel.posts.collectAsState()
            val post = posts.find { it.id == postId }

            if (post != null && post.mediaUrls.isNotEmpty()) {
                MediaViewerScreen(
                    mediaUrl = post.mediaUrls.first(),
                    mediaType = post.mediaType,
                    caption = post.caption,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.MessageMediaViewer.route,
            arguments = listOf(navArgument("messageId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: return@composable
            val messages by messageViewModel.currentMessages.collectAsState()
            val message = messages.find { it.id == messageId }

            if (message != null && message.mediaUrl != null) {
                GenericMediaViewerScreen(
                    mediaUrl = message.mediaUrl,
                    mediaType = message.type.name,
                    description = message.content.takeIf { it.isNotBlank() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.SharedMediaViewer.route,
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
            val media by spaceViewModel.media.collectAsState()
            val mediaItem = media.find { it.id == mediaId }

            if (mediaItem != null) {
                GenericMediaViewerScreen(
                    mediaUrl = mediaItem.url,
                    mediaType = mediaItem.type.name,
                    title = mediaItem.title,
                    description = mediaItem.description,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Memories.route) {
            MemoriesScreen(
                viewModel = spaceViewModel,
                preferencesViewModel = preferencesViewModel,
                currentUser = currentUser,
                onSpaceClick = { spaceId ->
                    navController.navigate(Screen.MemorySpace.createRoute(spaceId))
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.MemorySpace.route,
            arguments = listOf(navArgument("spaceId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val spaceId = backStackEntry.arguments?.getString("spaceId") ?: return@composable
            MemorySpaceScreen(
                spaceId = spaceId,
                viewModel = spaceViewModel,
                userViewModel = userViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onAddMedia = {
                    navController.navigate(Screen.AddMedia.createRoute(spaceId))
                },
                onNavigateToMediaViewer = { mediaId ->
                    navController.navigate(Screen.SharedMediaViewer.createRoute(mediaId))
                }
            )
        }

        composable(
            route = Screen.AddMedia.route,
            arguments = listOf(navArgument("spaceId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val spaceId = backStackEntry.arguments?.getString("spaceId") ?: return@composable
            AddMediaScreen(
                spaceId = spaceId,
                viewModel = spaceViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() },
                onMediaAdded = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = userViewModel,
                postViewModel = postViewModel,
                currentUser = currentUser,
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route)
                },
                onNavigateToMemories = {
                    navController.navigate(Screen.Memories.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onPostClick = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            SettingsScreen(
                preferencesViewModel = preferencesViewModel,
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onLogout = {
                    userViewModel.setCurrentUser(null)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PrivacySettings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            PrivacySettingsScreen(
                preferencesViewModel = preferencesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Écrans d'appels
        composable(
            route = Screen.Call.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            CallScreen(
                userId = userId,
                navController = navController,
                callViewModel = callViewModel,
                userViewModel = userViewModel
            )
        }

        composable(
            route = Screen.VideoCall.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            VideoCallScreen(
                userId = userId,
                navController = navController,
                callViewModel = callViewModel,
                userViewModel = userViewModel
            )
        }

        composable(
            route = Screen.CallHistory.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            CallHistoryScreen(
                navController = navController,
                callViewModel = callViewModel,
                userViewModel = userViewModel
            )
        }

        // Écrans de contact et médias
        composable(
            route = Screen.ContactDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ContactDetailScreen(
                userId = userId,
                navController = navController,
                userViewModel = userViewModel,
                messageViewModel = messageViewModel,
                callViewModel = callViewModel
            )
        }

        composable(
            route = Screen.ConversationMedia.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            ConversationMediaScreen(
                conversationId = conversationId,
                navController = navController,
                messageViewModel = messageViewModel
            )
        }

        // Écrans de messages importants
        composable(
            route = Screen.StarredMessages.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            StarredMessagesScreen(
                conversationId = conversationId,
                navController = navController,
                messageViewModel = messageViewModel,
                userViewModel = userViewModel
            )
        }

        composable(
            route = Screen.AllStarredMessages.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            AllStarredMessagesScreen(
                navController = navController,
                messageViewModel = messageViewModel,
                userViewModel = userViewModel
            )
        }

        // Conversations archivées
        composable(
            route = Screen.ArchivedConversations.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            ArchivedConversationsScreen(
                navController = navController,
                messageViewModel = messageViewModel,
                userViewModel = userViewModel
            )
        }

        // Forward messages
        composable(
            route = Screen.ForwardMessages.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            ForwardMessagesScreen(
                navController = navController,
                messageViewModel = messageViewModel,
                userViewModel = userViewModel
            )
        }

        // Caméra
        composable(
            route = Screen.Camera.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            CameraScreen(
                navController = navController,
                onPhotoCaptured = { photoPath ->
                    // TODO: Gérer la photo capturée
                    navController.popBackStack()
                },
                onVideoCaptured = { videoPath ->
                    // TODO: Gérer la vidéo capturée
                    navController.popBackStack()
                }
            )
        }
    }
}
