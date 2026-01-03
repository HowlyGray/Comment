package com.memoryshare.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.memoryshare.app.ui.screens.*
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.PostViewModel
import com.memoryshare.app.ui.viewmodel.SharedSpaceViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    userViewModel: UserViewModel,
    messageViewModel: MessageViewModel,
    postViewModel: PostViewModel,
    spaceViewModel: SharedSpaceViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()

    val startDestination = if (currentUser != null) {
        Screen.Messages.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                currentUser = currentUser,
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.MessageDetail.createRoute(conversationId))
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

        composable(
            route = Screen.MessageDetail.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            MessageDetailScreen(
                conversationId = conversationId,
                viewModel = messageViewModel,
                currentUser = currentUser,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Feed.route) {
            FeedScreen(
                viewModel = postViewModel,
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
