package com.memoryshare.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.memoryshare.app.ui.screens.*
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.PreferencesViewModel
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController,
    userViewModel: UserViewModel,
    messageViewModel: MessageViewModel,
    postViewModel: PostViewModel,
    spaceViewModel: SharedSpaceViewModel,
    preferencesViewModel: PreferencesViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Toujours démarrer sur Login, puis naviguer automatiquement si un utilisateur est restauré
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
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
                }
            )
        }

        composable(Screen.SelectContact.route) {
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

        composable(Screen.CreateGroup.route) {
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

        composable(Screen.AddContact.route) {
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
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            MessageDetailScreen(
                conversationId = conversationId,
                viewModel = messageViewModel,
                userViewModel = userViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Feed.route) {
            FeedScreen(
                viewModel = postViewModel,
                preferencesViewModel = preferencesViewModel,
                currentUser = currentUser,
                onPostClick = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToMemories = {
                    navController.navigate(Screen.Memories.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            PostDetailScreen(
                postId = postId,
                viewModel = postViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() }
            )
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
                }
            )
        }

        composable(
            route = Screen.MemorySpace.route,
            arguments = listOf(navArgument("spaceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val spaceId = backStackEntry.arguments?.getString("spaceId") ?: return@composable
            MemorySpaceScreen(
                spaceId = spaceId,
                viewModel = spaceViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = userViewModel,
                preferencesViewModel = preferencesViewModel,
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
                onLogout = {
                    userViewModel.setCurrentUser(null)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
