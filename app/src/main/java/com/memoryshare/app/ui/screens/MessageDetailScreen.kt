package com.memoryshare.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.theme.*
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
    onNavigateToVoiceCall: (String) -> Unit = {},
    onNavigateToForward: () -> Unit,
    onNavigateToMediaViewer: (String) -> Unit = {}
) {
    val messages by viewModel.currentMessages.collectAsState()
    val conversation by viewModel.currentConversation.collectAsState()
    val isLoading by viewModel.isLoadingMessages.collectAsState()
    val allUsers by userViewModel.users.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showMessageOptionsMenu by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var messageToReact by remember { mutableStateOf<Message?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessages by remember { mutableStateOf<Set<String>>(emptySet()) }

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

    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            messageText = editingMessage!!.content
        }
    }

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
                TopAppBar(
                    title = {
                        Text(
                            "${selectedMessages.size} sélectionné(s)",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Annuler")
                        }
                    },
                    actions = {
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

                        IconButton(onClick = {
                            val selectedMsgs = messages.filter { selectedMessages.contains(it.id) }
                            val allStarred = selectedMsgs.all { it.isStarred }
                            viewModel.starMultipleMessages(selectedMessages.toList(), !allStarred)
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            val selectedMsgs = messages.filter { selectedMessages.contains(it.id) }
                            val allStarred = selectedMsgs.all { it.isStarred }
                            Icon(
                                imageVector = if (allStarred) Icons.Default.StarBorder else Icons.Default.Star,
                                contentDescription = if (allStarred) "Retirer important" else "Marquer important",
                                tint = if (!allStarred) StarYellow else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            viewModel.setMessagesToForward(selectedMessages.toList())
                            onNavigateToForward()
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Forward, contentDescription = "Transférer")
                        }

                        IconButton(onClick = {
                            viewModel.deleteMultipleMessages(selectedMessages.toList())
                            isSelectionMode = false
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = ErrorRose)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                otherUser?.let { user -> onNavigateToContactDetail(user.id) }
                            }
                        ) {
                            UserAvatar(user = otherUser, size = 36.dp, showStoryRing = true)
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
                                        style = MaterialTheme.typography.labelSmall,
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
                            otherUser?.let { user -> onNavigateToVideoCall(user.id) }
                        }) {
                            Icon(Icons.Outlined.Videocam, contentDescription = "Appel vidéo", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = {
                            otherUser?.let { user -> onNavigateToVoiceCall(user.id) }
                        }) {
                            Icon(Icons.Outlined.Call, contentDescription = "Appel vocal", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Plus", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Afficher le contact") },
                                onClick = { showOptionsMenu = false; otherUser?.let { onNavigateToContactDetail(it.id) } },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Recherche") },
                                onClick = { showOptionsMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Médias, liens et documents") },
                                onClick = { showOptionsMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.Collections, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Thème de la discussion") },
                                onClick = { showOptionsMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Signaler") },
                                onClick = { showOptionsMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Bloquer") },
                                onClick = { showOptionsMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Effacer le contenu") },
                                onClick = { showOptionsMenu = false },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column {
                    // Reply indicator
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
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(VioletPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Répondre à",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VioletPrimary
                                    )
                                    Text(
                                        text = replyingToMessage!!.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { replyingToMessage = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Annuler", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Edit indicator
                    if (editingMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = VioletPrimary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = VioletPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Modifier le message",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VioletPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    editingMessage = null
                                    messageText = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Annuler", modifier = Modifier.size(18.dp))
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

                    // Input bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (editingMessage == null) {
                            IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                                Icon(
                                    imageVector = if (showAttachmentMenu) Icons.Default.Close else Icons.Outlined.Add,
                                    contentDescription = "Attacher",
                                    tint = CoralPrimary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    when {
                                        editingMessage != null -> "Modifier le message..."
                                        replyingToMessage != null -> "Répondre..."
                                        else -> "Message..."
                                    }
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                IconButton(onClick = { }) {
                                    Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emojis", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (messageText.isBlank() && editingMessage == null) {
                            IconButton(onClick = { }) {
                                Icon(Icons.Outlined.Mic, contentDescription = "Mémo vocal", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            // Gradient send button
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() && currentUser != null) {
                                        when {
                                            editingMessage != null -> {
                                                viewModel.editMessage(editingMessage!!.id, messageText)
                                                editingMessage = null
                                                messageText = ""
                                            }
                                            replyingToMessage != null -> {
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
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(CoralPrimary, VioletPrimary)
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = if (editingMessage != null) Icons.Default.Check else Icons.Default.Send,
                                    contentDescription = if (editingMessage != null) "Valider" else "Envoyer",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp),
                state = listState
            ) {
                if (isLoading && messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CoralPrimary)
                        }
                    }
                } else {
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
                            },
                            onMediaClick = {
                                onNavigateToMediaViewer(message.id)
                            }
                        )
                    }
                }
            }

            // Message options dialog
            if (showMessageOptionsMenu && selectedMessage != null) {
                val isOwnMessage = selectedMessage?.senderId == currentUser?.id

                AlertDialog(
                    onDismissRequest = {
                        showMessageOptionsMenu = false
                        selectedMessage = null
                    },
                    title = { Text("Actions", fontWeight = FontWeight.Bold) },
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
                                    Icon(Icons.Outlined.Edit, contentDescription = null)
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
                                Icon(Icons.Outlined.Reply, contentDescription = null)
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
                                Icon(Icons.Outlined.EmojiEmotions, contentDescription = null)
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
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = ErrorRose)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Supprimer", color = ErrorRose)
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
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

            // Emoji picker
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
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(icon = Icons.Outlined.PhotoLibrary, label = "Galerie", onClick = onGallery, gradientColors = listOf(CoralPrimary.copy(alpha = 0.15f), CoralLight.copy(alpha = 0.15f)))
                AttachmentOption(icon = Icons.Outlined.CameraAlt, label = "Caméra", onClick = onCamera, gradientColors = listOf(VioletPrimary.copy(alpha = 0.15f), VioletLight.copy(alpha = 0.15f)))
                AttachmentOption(icon = Icons.Outlined.Headphones, label = "Audio", onClick = onAudio, gradientColors = listOf(GradientSunset.copy(alpha = 0.15f), CoralPrimary.copy(alpha = 0.15f)))
                AttachmentOption(icon = Icons.Outlined.Description, label = "Document", onClick = onDocument, gradientColors = listOf(InfoBlue.copy(alpha = 0.15f), VioletPrimary.copy(alpha = 0.15f)))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(icon = Icons.Outlined.Contacts, label = "Contact", onClick = onContact, gradientColors = listOf(SuccessGreen.copy(alpha = 0.15f), InfoBlue.copy(alpha = 0.15f)))
                AttachmentOption(icon = Icons.Outlined.Poll, label = "Sondage", onClick = onPoll, gradientColors = listOf(WarningAmber.copy(alpha = 0.15f), CoralPrimary.copy(alpha = 0.15f)))
                AttachmentOption(icon = Icons.Outlined.Event, label = "Événement", onClick = onEvent, gradientColors = listOf(VioletPrimary.copy(alpha = 0.15f), GradientSunset.copy(alpha = 0.15f)))
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
    gradientColors: List<Color> = listOf(CoralPrimary.copy(alpha = 0.15f), VioletPrimary.copy(alpha = 0.15f))
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
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
    onReactionClick: () -> Unit,
    onMediaClick: () -> Unit = {}
) {
    val reactions by messageViewModel.getReactionsForMessage(message.id).collectAsState(initial = emptyList())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.align(Alignment.CenterVertically),
                colors = CheckboxDefaults.colors(checkedColor = CoralPrimary, checkmarkColor = Color.White)
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
            // Reply reference
            if (message.replyToId != null) {
                Surface(
                    modifier = Modifier.padding(bottom = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(VioletPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Réponse à un message",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Message bubble
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
                } else if (!isFromCurrentUser) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                }
            ) {
                Box(
                    modifier = if (isFromCurrentUser && !isSelected) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(BubbleSentStart, BubbleSentEnd)
                            )
                        )
                    } else {
                        Modifier
                    }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        when (message.type) {
                            MessageType.TEXT -> {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            MessageType.IMAGE -> {
                                if (message.mediaUrl != null) {
                                    AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = message.content,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onMediaClick() },
                                        contentScale = ContentScale.Crop
                                    )
                                    if (message.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = message.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Text(text = "[Image]", style = MaterialTheme.typography.bodyMedium, color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            MessageType.VIDEO -> {
                                if (message.mediaUrl != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onMediaClick() }
                                    ) {
                                        AsyncImage(
                                            model = message.mediaThumbnailUrl ?: message.mediaUrl,
                                            contentDescription = message.content,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(CoralPrimary.copy(alpha = 0.85f), VioletPrimary.copy(alpha = 0.85f))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Lecture",
                                                modifier = Modifier.size(28.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    if (message.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = message.content, style = MaterialTheme.typography.bodySmall, color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface)
                                    }
                                } else {
                                    Text(text = "[Vidéo]", style = MaterialTheme.typography.bodyMedium, color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            MessageType.AUDIO -> {
                                if (message.mediaUrl != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onMediaClick() }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Mic,
                                            contentDescription = "Audio",
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = message.content.ifBlank { "Message vocal" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Text(text = "[Audio]", style = MaterialTheme.typography.bodyMedium, color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            MessageType.FILE -> {
                                Text(text = "[Fichier]", style = MaterialTheme.typography.bodyMedium, color = if (isFromCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDetailTime(message.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFromCurrentUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (message.editedAt != null) {
                                Text(
                                    text = "• modifié",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isFromCurrentUser) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            if (message.isStarred) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Message important",
                                    modifier = Modifier.size(14.dp),
                                    tint = StarYellow
                                )
                            }
                        }
                    }
                }
            }

            // Reactions
            if (reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                                Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                                Text(text = emojiReactions.size.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Add reaction button
            IconButton(
                onClick = onReactionClick,
                modifier = Modifier.size(28.dp).padding(top = 2.dp)
            ) {
                Icon(
                    Icons.Outlined.AddReaction,
                    contentDescription = "Ajouter réaction",
                    modifier = Modifier.size(14.dp),
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
                        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
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

private fun formatDetailTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
