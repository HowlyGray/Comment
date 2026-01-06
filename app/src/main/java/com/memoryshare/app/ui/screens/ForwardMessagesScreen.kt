package com.memoryshare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardMessagesScreen(
    navController: NavController,
    messageViewModel: MessageViewModel,
    userViewModel: UserViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val conversations by messageViewModel.conversations.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    val messages by messageViewModel.currentMessages.collectAsState()
    val messageIds by messageViewModel.messagesToForward.collectAsState()

    // Filter messages to forward
    val messagesToForward = remember(messages, messageIds) {
        messages.filter { messageIds.contains(it.id) }
    }

    // Recent conversations (excluding group chats for simplicity)
    val recentConversations = remember(conversations, currentUser) {
        conversations.filter { !it.isGroup }
            .sortedByDescending { it.lastMessageTime ?: 0 }
            .take(10)
    }

    // Get users from recent conversations
    val recentUserIds = remember(recentConversations, currentUser) {
        recentConversations.flatMap { conv ->
            conv.participantIds.filter { it != currentUser?.id }
        }.distinct()
    }

    val recentUsers = remember(allUsers, recentUserIds) {
        allUsers.filter { recentUserIds.contains(it.id) }
    }

    // All other users (excluding current user and recent users)
    val otherUsers = remember(allUsers, currentUser, recentUserIds) {
        allUsers.filter { user ->
            user.id != currentUser?.id && !recentUserIds.contains(user.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transférer ${messagesToForward.size} message(s)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Section: Recent Conversations
            if (recentUsers.isNotEmpty()) {
                item {
                    Text(
                        text = "Discussions récentes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(recentUsers) { user ->
                    ForwardContactItem(
                        user = user,
                        onClick = {
                            // Find or create conversation with this user
                            currentUser?.let { current ->
                                val existingConv = conversations.find { conv ->
                                    !conv.isGroup &&
                                    conv.participantIds.contains(current.id) &&
                                    conv.participantIds.contains(user.id)
                                }

                                val conversationId = existingConv?.id ?: run {
                                    // Create new conversation if it doesn't exist
                                    messageViewModel.createConversation(
                                        participantIds = listOf(current.id, user.id),
                                        isGroup = false
                                    )
                                    // Return a temporary ID (the actual ID will be generated in the repository)
                                    UUID.randomUUID().toString()
                                }

                                // Forward messages to this conversation
                                messageViewModel.forwardMessages(
                                    messageIds = messageIds,
                                    targetConversationId = existingConv?.id ?: conversationId,
                                    senderId = current.id
                                )

                                // Navigate back
                                navController.popBackStack()
                            }
                        }
                    )
                    Divider()
                }
            }

            // Section: All Contacts
            if (otherUsers.isNotEmpty()) {
                item {
                    Text(
                        text = "Tous les contacts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(otherUsers) { user ->
                    ForwardContactItem(
                        user = user,
                        onClick = {
                            // Find or create conversation with this user
                            currentUser?.let { current ->
                                val existingConv = conversations.find { conv ->
                                    !conv.isGroup &&
                                    conv.participantIds.contains(current.id) &&
                                    conv.participantIds.contains(user.id)
                                }

                                val conversationId = existingConv?.id ?: run {
                                    // Create new conversation if it doesn't exist
                                    messageViewModel.createConversation(
                                        participantIds = listOf(current.id, user.id),
                                        isGroup = false
                                    )
                                    // Return a temporary ID (the actual ID will be generated in the repository)
                                    UUID.randomUUID().toString()
                                }

                                // Forward messages to this conversation
                                messageViewModel.forwardMessages(
                                    messageIds = messageIds,
                                    targetConversationId = existingConv?.id ?: conversationId,
                                    senderId = current.id
                                )

                                // Navigate back
                                navController.popBackStack()
                            }
                        }
                    )
                    Divider()
                }
            }

            // Empty state
            if (recentUsers.isEmpty() && otherUsers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Aucun contact disponible",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForwardContactItem(
    user: User,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(user = user, size = 48.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                user.bio?.let { bio ->
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
