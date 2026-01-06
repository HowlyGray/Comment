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
    object AddMedia : Screen("memories/{spaceId}/add") {
        fun createRoute(spaceId: String) = "memories/$spaceId/add"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Call : Screen("call/{userId}") {
        fun createRoute(userId: String) = "call/$userId"
    }
    object VideoCall : Screen("video_call/{userId}") {
        fun createRoute(userId: String) = "video_call/$userId"
    }
    object ContactDetail : Screen("contact/{userId}") {
        fun createRoute(userId: String) = "contact/$userId"
    }
    object ConversationMedia : Screen("conversation_media/{conversationId}") {
        fun createRoute(conversationId: String) = "conversation_media/$conversationId"
    }
    object StarredMessages : Screen("starred_messages/{conversationId}") {
        fun createRoute(conversationId: String) = "starred_messages/$conversationId"
    }
    object AllStarredMessages : Screen("all_starred_messages")
    object ArchivedConversations : Screen("archived_conversations")
    object Camera : Screen("camera")
    object CallHistory : Screen("call_history")
    object ForwardMessages : Screen("forward_messages")
}
