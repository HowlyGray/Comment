package com.memoryshare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.BottomNavigationBar
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: MessageViewModel,
    userViewModel: com.memoryshare.app.ui.viewmodel.UserViewModel,
    preferencesViewModel: com.memoryshare.app.ui.viewmodel.PreferencesViewModel,
    currentUser: User?,
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToArchived: () -> Unit = {},
    onNavigateToAllStarred: () -> Unit = {}
) {
    val conversations by viewModel.conversations.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    val fabOnLeft by preferencesViewModel.fabOnLeft.collectAsState()
    var showOptionsMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Messages importants") },
                            onClick = {
                                showOptionsMenu = false
                                onNavigateToAllStarred()
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Conversations archivées") },
                            onClick = {
                                showOptionsMenu = false
                                onNavigateToArchived()
                            },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            onClick = {
                                showOptionsMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = 0,
                onNavigateToMessages = {},
                onNavigateToFeed = onNavigateToFeed,
                onNavigateToMemories = onNavigateToMemories,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Contenu principal
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aucune conversation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Commencez une nouvelle conversation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Bouton Archivées
                    item {
                        Surface(
                            onClick = onNavigateToArchived,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Archivées",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Divider()
                    }

                    items(conversations) { conversation ->
                        val displayName = getConversationDisplayName(conversation, currentUser, allUsers)
                        val otherUser = if (!conversation.isGroup && conversation.participantIds.size == 2) {
                            val otherUserId = conversation.participantIds.find { it != currentUser?.id }
                            allUsers.find { it.id == otherUserId }
                        } else null

                        ConversationItem(
                            conversation = conversation,
                            displayName = displayName,
                            otherUser = otherUser,
                            onClick = { onConversationClick(conversation.id) }
                        )
                        Divider()
                    }
                }
            }

            // FAB positionné manuellement
            FloatingActionButton(
                onClick = onNewConversation,
                modifier = Modifier
                    .align(if (fabOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 96.dp) // Padding supplémentaire pour éviter la barre de navigation
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle conversation")
            }
        }
    }
}

fun getConversationDisplayName(conversation: Conversation, currentUser: User?, allUsers: List<User>): String {
    return if (conversation.isGroup || conversation.name != null) {
        conversation.name ?: "Groupe"
    } else {
        // Conversation 1-to-1: trouver l'autre utilisateur
        val otherUserId = conversation.participantIds.find { it != currentUser?.id }
        val otherUser = allUsers.find { it.id == otherUserId }
        otherUser?.displayName ?: "Contact"
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    displayName: String,
    otherUser: User?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            user = otherUser,
            size = 56.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = conversation.lastMessageText ?: "Aucun message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (conversation.lastMessageTime != null) {
            Text(
                text = formatTime(conversation.lastMessageTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
