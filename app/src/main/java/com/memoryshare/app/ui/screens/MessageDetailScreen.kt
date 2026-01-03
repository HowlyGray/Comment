package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    conversationId: String,
    viewModel: MessageViewModel,
    userViewModel: UserViewModel,
    currentUser: User?,
    onBack: () -> Unit
) {
    val messages by viewModel.currentMessages.collectAsState()
    val conversation by viewModel.currentConversation.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
        viewModel.loadMessages(conversationId)
        viewModel.markAsRead(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Trouver l'autre utilisateur pour une conversation 1-to-1
    val otherUser = if (conversation != null && !conversation!!.isGroup && conversation!!.participantIds.size == 2) {
        val otherUserId = conversation!!.participantIds.find { it != currentUser?.id }
        allUsers.find { it.id == otherUserId }
    } else null

    val displayName = if (conversation?.isGroup == true || conversation?.name != null) {
        conversation?.name ?: "Groupe"
    } else {
        otherUser?.displayName ?: "Contact"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(user = otherUser, size = 36.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = displayName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (otherUser?.bio != null) {
                                Text(
                                    text = otherUser.bio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Video call */ }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Appel vidéo")
                    }
                    IconButton(onClick = { /* Voice call */ }) {
                        Icon(Icons.Default.Call, contentDescription = "Appel vocal")
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Plus")
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Afficher le contact") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Navigate to contact profile
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Recherche") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Search in conversation
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Médias, liens et documents") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Show media gallery
                            },
                            leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Thème de la discussion") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Change theme
                            },
                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Signaler") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Report conversation
                            },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Bloquer") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Block contact
                            },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Effacer le contenu") },
                            onClick = {
                                showOptionsMenu = false
                                // TODO: Clear conversation
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column {
                    if (showAttachmentMenu) {
                        AttachmentOptionsGrid(
                            onDismiss = { showAttachmentMenu = false },
                            onGallery = { showAttachmentMenu = false },
                            onCamera = { showAttachmentMenu = false },
                            onAudio = { showAttachmentMenu = false },
                            onDocument = { showAttachmentMenu = false },
                            onContact = { showAttachmentMenu = false },
                            onPoll = { showAttachmentMenu = false },
                            onEvent = { showAttachmentMenu = false }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                            Icon(
                                imageVector = if (showAttachmentMenu) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Attacher",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message...") },
                            maxLines = 4,
                            trailingIcon = {
                                IconButton(onClick = { /* TODO: Show emoji picker */ }) {
                                    Icon(Icons.Default.EmojiEmotions, contentDescription = "Emojis")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        if (messageText.isBlank()) {
                            IconButton(onClick = { /* TODO: Record voice note */ }) {
                                Icon(Icons.Default.Mic, contentDescription = "Mémo vocal")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() && currentUser != null) {
                                        viewModel.sendMessage(
                                            conversationId = conversationId,
                                            senderId = currentUser.id,
                                            content = messageText,
                                            type = MessageType.TEXT
                                        )
                                        messageText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Envoyer")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            state = listState
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isFromCurrentUser = message.senderId == currentUser?.id
                )
            }
        }
    }
}

@Composable
fun AttachmentOptionsGrid(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onAudio: () -> Unit,
    onDocument: () -> Unit,
    onContact: () -> Unit,
    onPoll: () -> Unit,
    onEvent: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Galerie",
                    onClick = onGallery,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                AttachmentOption(
                    icon = Icons.Default.CameraAlt,
                    label = "Caméra",
                    onClick = onCamera,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
                AttachmentOption(
                    icon = Icons.Default.Headphones,
                    label = "Audio",
                    onClick = onAudio,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
                AttachmentOption(
                    icon = Icons.Default.Description,
                    label = "Document",
                    onClick = onDocument,
                    color = MaterialTheme.colorScheme.errorContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Default.Contacts,
                    label = "Contact",
                    onClick = onContact,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
                AttachmentOption(
                    icon = Icons.Default.Poll,
                    label = "Sondage",
                    onClick = onPoll,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
                AttachmentOption(
                    icon = Icons.Default.Event,
                    label = "Événement",
                    onClick = onEvent,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                )
                Spacer(modifier = Modifier.width(64.dp))
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = color,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            UserAvatar(user = null, size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                ),
                color = if (isFromCurrentUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        MessageType.IMAGE -> {
                            Text(text = "[Image]", style = MaterialTheme.typography.bodyMedium)
                        }
                        MessageType.VIDEO -> {
                            Text(text = "[Vidéo]", style = MaterialTheme.typography.bodyMedium)
                        }
                        MessageType.AUDIO -> {
                            Text(text = "[Audio]", style = MaterialTheme.typography.bodyMedium)
                        }
                        MessageType.FILE -> {
                            Text(text = "[Fichier]", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            UserAvatar(user = null, size = 32.dp)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
