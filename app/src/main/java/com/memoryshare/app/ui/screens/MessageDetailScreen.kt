package com.memoryshare.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onBack: () -> Unit,
    onNavigateToContactDetail: (String) -> Unit = {},
    onNavigateToVideoCall: (String) -> Unit = {},
    onNavigateToVoiceCall: (String) -> Unit = {}
) {
    val messages by viewModel.currentMessages.collectAsState()
    val conversation by viewModel.currentConversation.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // États pour édition, réponse et réactions
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showMessageOptionsMenu by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var messageToReact by remember { mutableStateOf<Message?>(null) }

    // États pour le mode sélection multiple
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessages by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Exit selection mode when no items are selected
    LaunchedEffect(selectedMessages) {
        if (selectedMessages.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

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

    // Remplir le champ texte lors de l'édition
    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            messageText = editingMessage!!.content
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
            if (isSelectionMode) {
                // Selection mode top bar
                TopAppBar(
                    title = { Text("${selectedMessages.size} sélectionné(s)") },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Annuler")
                        }
                    },
                    actions = {
                        // Reply button (only if one message selected)
                        if (selectedMessages.size == 1) {
                            IconButton(onClick = {
                                val msg = messages.find { it.id == selectedMessages.first() }
                                msg?.let {
                                    replyingToMessage = it
                                    isSelectionMode = false
                                    selectedMessages = emptySet()
                                }
                            }) {
                                Icon(Icons.Default.Reply, contentDescription = "Répondre")
                            }
                        }

                        // Star/Unstar toggle button
                        IconButton(onClick = {
                            val selectedMsgs = messages.filter { selectedMessages.contains(it.id) }
                            val allStarred = selectedMsgs.all { it.isStarred }
                            // If all selected messages are starred, unstar them; otherwise star them
                            viewModel.starMultipleMessages(selectedMessages.toList(), !allStarred)
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            val selectedMsgs = messages.filter { selectedMessages.contains(it.id) }
                            val allStarred = selectedMsgs.all { it.isStarred }
                            Icon(
                                imageVector = if (allStarred) Icons.Default.StarBorder else Icons.Default.Star,
                                contentDescription = if (allStarred) "Retirer important" else "Marquer important",
                                tint = if (allStarred) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Forward button
                        IconButton(onClick = {
                            // Store selected message IDs in ViewModel for ForwardMessagesScreen
                            viewModel.setMessagesToForward(selectedMessages.toList())
                            navController.navigate("forward_messages")
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Forward, contentDescription = "Transférer")
                        }

                        // Delete button
                        IconButton(onClick = {
                            viewModel.deleteMultipleMessages(selectedMessages.toList())
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                        }
                    }
                )
            } else {
                // Normal mode top bar
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                // Navigate to contact detail (only for 1-to-1 conversations)
                                otherUser?.let { user ->
                                    onNavigateToContactDetail(user.id)
                                }
                            }
                        ) {
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
                        IconButton(onClick = {
                            otherUser?.let { user ->
                                onNavigateToVideoCall(user.id)
                            }
                        }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Appel vidéo")
                        }
                        IconButton(onClick = {
                            otherUser?.let { user ->
                                onNavigateToVoiceCall(user.id)
                            }
                        }) {
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
                                    otherUser?.let { user ->
                                        onNavigateToContactDetail(user.id)
                                    }
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
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column {
                    // Indicateur de réponse
                    if (replyingToMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Reply,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Répondre à",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = replyingToMessage!!.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { replyingToMessage = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Annuler")
                                }
                            }
                        }
                    }

                    // Indicateur d'édition
                    if (editingMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Modifier le message",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    editingMessage = null
                                    messageText = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Annuler")
                                }
                            }
                        }
                    }

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
                        if (editingMessage == null) {
                            IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                                Icon(
                                    imageVector = if (showAttachmentMenu) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = "Attacher",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(
                                when {
                                    editingMessage != null -> "Modifier le message..."
                                    replyingToMessage != null -> "Répondre..."
                                    else -> "Message..."
                                }
                            ) },
                            maxLines = 4,
                            trailingIcon = {
                                IconButton(onClick = { /* TODO: Show emoji picker */ }) {
                                    Icon(Icons.Default.EmojiEmotions, contentDescription = "Emojis")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        if (messageText.isBlank() && editingMessage == null) {
                            IconButton(onClick = { /* TODO: Record voice note */ }) {
                                Icon(Icons.Default.Mic, contentDescription = "Mémo vocal")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() && currentUser != null) {
                                        when {
                                            editingMessage != null -> {
                                                // Éditer le message
                                                viewModel.editMessage(editingMessage!!.id, messageText)
                                                editingMessage = null
                                                messageText = ""
                                            }
                                            replyingToMessage != null -> {
                                                // Envoyer une réponse
                                                viewModel.replyToMessage(
                                                    conversationId = conversationId,
                                                    senderId = currentUser.id,
                                                    content = messageText,
                                                    replyToMessageId = replyingToMessage!!.id
                                                )
                                                replyingToMessage = null
                                                messageText = ""
                                            }
                                            else -> {
                                                // Envoyer un message normal
                                                viewModel.sendMessage(
                                                    conversationId = conversationId,
                                                    senderId = currentUser.id,
                                                    content = messageText,
                                                    type = MessageType.TEXT
                                                )
                                                messageText = ""
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (editingMessage != null) Icons.Default.Check else Icons.Default.Send,
                                    contentDescription = if (editingMessage != null) "Valider" else "Envoyer"
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                        isFromCurrentUser = message.senderId == currentUser?.id,
                        currentUser = currentUser,
                        allUsers = allUsers,
                        messageViewModel = viewModel,
                        isSelected = selectedMessages.contains(message.id),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                // Toggle selection
                                selectedMessages = if (selectedMessages.contains(message.id)) {
                                    selectedMessages - message.id
                                } else {
                                    selectedMessages + message.id
                                }
                            }
                        },
                        onLongPress = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedMessages = setOf(message.id)
                            } else {
                                selectedMessage = message
                                showMessageOptionsMenu = true
                            }
                        },
                        onReactionClick = {
                            messageToReact = message
                            showEmojiPicker = true
                        }
                    )
                }
            }

            // Menu d'options du message
            if (showMessageOptionsMenu && selectedMessage != null) {
                val isOwnMessage = selectedMessage?.senderId == currentUser?.id

                AlertDialog(
                    onDismissRequest = {
                        showMessageOptionsMenu = false
                        selectedMessage = null
                    },
                    title = { Text("Actions") },
                    text = {
                        Column {
                            if (isOwnMessage) {
                                TextButton(
                                    onClick = {
                                        editingMessage = selectedMessage
                                        showMessageOptionsMenu = false
                                        selectedMessage = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Modifier")
                                }
                            }

                            TextButton(
                                onClick = {
                                    replyingToMessage = selectedMessage
                                    showMessageOptionsMenu = false
                                    selectedMessage = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Reply, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Répondre")
                            }

                            TextButton(
                                onClick = {
                                    messageToReact = selectedMessage
                                    showEmojiPicker = true
                                    showMessageOptionsMenu = false
                                    selectedMessage = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Réagir")
                            }

                            if (isOwnMessage) {
                                TextButton(
                                    onClick = {
                                        selectedMessage?.let { viewModel.deleteMessage(it) }
                                        showMessageOptionsMenu = false
                                        selectedMessage = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showMessageOptionsMenu = false
                            selectedMessage = null
                        }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Sélecteur d'emoji pour réactions
            if (showEmojiPicker && messageToReact != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showEmojiPicker = false
                        messageToReact = null
                    }
                ) {
                    EmojiPicker(
                        onEmojiSelected = { emoji ->
                            currentUser?.let { user ->
                                viewModel.addReaction(messageToReact!!.id, user.id, emoji)
                            }
                            showEmojiPicker = false
                            messageToReact = null
                        },
                        onDismiss = {
                            showEmojiPicker = false
                            messageToReact = null
                        }
                    )
                }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    currentUser: User?,
    allUsers: List<User>,
    messageViewModel: MessageViewModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit,
    onReactionClick: () -> Unit
) {
    val reactions by messageViewModel.getReactionsForMessage(message.id).collectAsState(initial = emptyList())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        // Show checkbox in selection mode (on the left for all messages)
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (!isFromCurrentUser) {
            val sender = allUsers.find { it.id == message.senderId }
            UserAvatar(user = sender, size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Message répondu (citation)
            if (message.replyToId != null) {
                Surface(
                    modifier = Modifier.padding(bottom = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Réponse à un message",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.combinedClickable(
                    onClick = { if (isSelectionMode) onClick() },
                    onLongClick = onLongPress
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else if (isFromCurrentUser) {
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(message.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (message.editedAt != null) {
                            Text(
                                text = "• modifié",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        // Show star icon if message is starred
                        if (message.isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Message important",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Display reactions grouped by emoji
            if (reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Group reactions by emoji and count them
                    val groupedReactions = reactions.groupBy { it.emoji }
                    groupedReactions.forEach { (emoji, emojiReactions) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = emojiReactions.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Add reaction button
            IconButton(
                onClick = onReactionClick,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Default.AddReaction,
                    contentDescription = "Ajouter réaction",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            currentUser?.let { UserAvatar(user = it, size = 32.dp) }
        }
    }
}

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val commonEmojis = listOf(
        "❤️", "👍", "👎", "😂", "😮", "😢", "😡", "🔥",
        "🎉", "👏", "💯", "✅", "❌", "🙏", "💪", "👀",
        "😍", "🤔", "😊", "🥳", "😎", "🤗", "🙌", "💙"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Choisir une réaction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(commonEmojis) { emoji ->
                Surface(
                    onClick = { onEmojiSelected(emoji) },
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Annuler")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
