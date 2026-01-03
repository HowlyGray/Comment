package com.memoryshare.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Messages : Screen("messages")
    object SelectContact : Screen("select_contact")
    object CreateGroup : Screen("create_group")
    object AddContact : Screen("add_contact")
    object MessageDetail : Screen("messages/{conversationId}") {
        fun createRoute(conversationId: String) = "messages/$conversationId"
    }
    object Feed : Screen("feed")
    object CreatePost : Screen("create_post")
    object PostDetail : Screen("feed/{postId}") {
        fun createRoute(postId: String) = "feed/$postId"
    }
    object Memories : Screen("memories")
    object MemorySpace : Screen("memories/{spaceId}") {
        fun createRoute(spaceId: String) = "memories/$spaceId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
