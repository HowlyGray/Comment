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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedConversationsScreen(
    navController: NavController,
    messageViewModel: MessageViewModel,
    userViewModel: UserViewModel
) {
    val archivedConversations = remember { mutableStateOf<List<Conversation>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        messageViewModel.getArchivedConversations().collect { conversations ->
            archivedConversations.value = conversations
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations archivées") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (archivedConversations.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Aucune conversation archivée",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Les conversations archivées apparaîtront ici",
                        fontSize = 14.sp,
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
                items(archivedConversations.value) { conversation ->
                    ArchivedConversationItem(
                        conversation = conversation,
                        userViewModel = userViewModel,
                        onUnarchive = {
                            messageViewModel.archiveConversation(conversation.id, false)
                        },
                        onClick = {
                            navController.navigate("messages/${conversation.id}")
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ArchivedConversationItem(
    conversation: Conversation,
    userViewModel: UserViewModel,
    onUnarchive: () -> Unit,
    onClick: () -> Unit
) {
    val currentUser by userViewModel.currentUser.collectAsState()

    // Pour les conversations 1-to-1, récupérer l'autre utilisateur
    val otherUserId = if (!conversation.isGroup && conversation.participantIds.size == 2) {
        conversation.participantIds.firstOrNull { it != currentUser?.id }
    } else null

    val otherUser by userViewModel.getUserById(otherUserId ?: "").collectAsState(initial = null)

    val displayName = if (conversation.isGroup) {
        conversation.name ?: "Groupe"
    } else {
        otherUser?.displayName ?: "Utilisateur"
    }

    val displayImageUrl = if (conversation.isGroup) {
        conversation.imageUrl
    } else {
        otherUser?.profilePictureUrl
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (conversation.isGroup) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            otherUser?.let { user ->
                UserAvatar(user = user, size = 56.dp)
            } ?: Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Contenu
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                conversation.lastMessageTime?.let { time ->
                    Text(
                        text = formatTime(time),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessageText ?: "Aucun message",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Bouton désarchiver
                IconButton(
                    onClick = onUnarchive
                ) {
                    Icon(
                        imageVector = Icons.Default.Unarchive,
                        contentDescription = "Désarchiver",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "À l'instant"
        diff < 3600000 -> "${diff / 60000} min"
        diff < 86400000 -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        diff < 604800000 -> {
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
