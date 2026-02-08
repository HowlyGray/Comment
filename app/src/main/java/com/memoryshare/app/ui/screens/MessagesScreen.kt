package com.memoryshare.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.memoryshare.app.data.model.Conversation
import com.memoryshare.app.data.model.User
import com.memoryshare.app.ui.components.BottomNavigationBar
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.theme.*
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val unreadArchivedCount by viewModel.getUnreadArchivedMessagesCount().collectAsState(initial = 0)

    var showOptionsMenu by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedConversations by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(selectedConversations) {
        if (selectedConversations.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

    val selectedConvs = conversations.filter { selectedConversations.contains(it.id) }
    val allPinned = selectedConvs.isNotEmpty() && selectedConvs.all { it.pinned }
    val allMuted = selectedConvs.isNotEmpty() && selectedConvs.all { it.muted }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedConversations.size} sélectionnée(s)",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedConversations = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Annuler")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (allPinned) {
                                viewModel.pinMultipleConversations(selectedConversations.toList(), false)
                            } else {
                                viewModel.pinMultipleConversations(selectedConversations.toList(), true)
                            }
                            isSelectionMode = false
                            selectedConversations = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (allPinned) "Désépingler" else "Épingler",
                                tint = if (allPinned) CoralPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            if (allMuted) {
                                viewModel.muteMultipleConversations(selectedConversations.toList(), false)
                            } else {
                                viewModel.muteMultipleConversations(selectedConversations.toList(), true)
                            }
                            isSelectionMode = false
                            selectedConversations = emptySet()
                        }) {
                            Icon(
                                imageVector = if (allMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (allMuted) "Réactiver le son" else "Mettre en sourdine"
                            )
                        }

                        IconButton(onClick = {
                            viewModel.archiveMultipleConversations(selectedConversations.toList(), true)
                            isSelectionMode = false
                            selectedConversations = emptySet()
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Archiver")
                        }

                        IconButton(onClick = {
                            viewModel.deleteMultipleConversations(selectedConversations.toList())
                            isSelectionMode = false
                            selectedConversations = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = ErrorRose)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "Messages",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Messages importants") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToAllStarred()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null, tint = StarYellow) }
                            )
                            DropdownMenuItem(
                                text = { Text("Conversations archivées") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToArchived()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Paramètres") },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = 0,
                onNavigateToMessages = {},
                onNavigateToFeed = onNavigateToFeed,
                onNavigateToMemories = onNavigateToMemories,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (conversations.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            CoralPrimary.copy(alpha = 0.15f),
                                            VioletPrimary.copy(alpha = 0.15f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = CoralPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Aucune conversation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lancez une discussion avec vos proches",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Archived conversations row
                    if (!isSelectionMode) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onNavigateToArchived)
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Archive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Archivées",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (unreadArchivedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(CoralPrimary, VioletPrimary)
                                                )
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unreadArchivedCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
                            isSelected = selectedConversations.contains(conversation.id),
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedConversations = if (selectedConversations.contains(conversation.id)) {
                                        selectedConversations - conversation.id
                                    } else {
                                        selectedConversations + conversation.id
                                    }
                                } else {
                                    onConversationClick(conversation.id)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedConversations = setOf(conversation.id)
                                }
                            }
                        )
                    }

                    // Bottom spacer for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // Gradient FAB
            if (!isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(if (fabOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    FloatingActionButton(
                        onClick = onNewConversation,
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CoralPrimary, VioletPrimary)
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Nouvelle conversation",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getConversationDisplayName(conversation: Conversation, currentUser: User?, allUsers: List<User>): String {
    return if (conversation.isGroup || conversation.name != null) {
        conversation.name ?: "Groupe"
    } else {
        val otherUserId = conversation.participantIds.find { it != currentUser?.id }
        val otherUser = allUsers.find { it.id == otherUserId }
        otherUser?.displayName ?: "Contact"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: Conversation,
    displayName: String,
    otherUser: User?,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CoralPrimary,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            UserAvatar(
                user = otherUser,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conversation.pinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Épinglée",
                            modifier = Modifier.size(14.dp),
                            tint = CoralPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (conversation.muted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "En sourdine",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = conversation.lastMessageText ?: "Aucun message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (conversation.lastMessageTime != null) {
                    Text(
                        text = formatTime(conversation.lastMessageTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
