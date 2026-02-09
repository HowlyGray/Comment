package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.memoryshare.app.data.model.CallType
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.navigation.Screen
import com.memoryshare.app.ui.theme.CoralPrimary
import com.memoryshare.app.ui.theme.VioletPrimary
import com.memoryshare.app.ui.viewmodel.CallViewModel
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    userId: String,
    navController: NavController,
    userViewModel: UserViewModel,
    messageViewModel: MessageViewModel,
    callViewModel: CallViewModel
) {
    val user by userViewModel.getUserById(userId).collectAsState(initial = null)
    val currentUser by userViewModel.currentUser.collectAsState()
    val conversations by messageViewModel.conversations.collectAsState()
    val followingUserIds by userViewModel.followingUserIds.collectAsState()
    val isFollowed = followingUserIds.contains(userId)
    val coroutineScope = rememberCoroutineScope()

    // Find conversation between current user and this contact
    val conversation = remember(conversations, currentUser, userId) {
        conversations.firstOrNull { conv ->
            !conv.isGroup &&
            conv.participantIds.size == 2 &&
            conv.participantIds.contains(currentUser?.id) &&
            conv.participantIds.contains(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        user?.let { contact ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // En-tête avec avatar et nom
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatar(
                            user = contact,
                            size = 120.dp,
                            showStoryRing = true
                        )

                        Text(
                            text = contact.displayName.ifBlank { contact.username },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "@${contact.username}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Biographie
                        contact.bio?.let { bio ->
                            Text(
                                text = bio,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Boutons d'action principaux
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bouton Message (DM)
                        Button(
                            onClick = {
                                currentUser?.let { current ->
                                    coroutineScope.launch {
                                        val conv = messageViewModel.findOrCreateConversation(
                                            participantIds = listOf(current.id, userId)
                                        )
                                        navController.navigate(Screen.MessageDetail.createRoute(conv.id)) {
                                            popUpTo(Screen.Feed.route) { inclusive = false }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(CoralPrimary, VioletPrimary)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Message, "Message", modifier = Modifier.size(20.dp), tint = Color.White)
                                    Text("Message", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Bouton Suivre / Suivi
                        OutlinedButton(
                            onClick = { userViewModel.toggleFollow(userId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (isFollowed) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isFollowed) "Suivi" else "Suivre")
                        }
                    }
                }

                // Boutons appels
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Appel vocal
                        OutlinedButton(
                            onClick = {
                                callViewModel.startCall(userId, CallType.VOICE)
                                navController.navigate("call/$userId")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, "Appeler", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Appeler")
                        }

                        // Appel vidéo
                        OutlinedButton(
                            onClick = {
                                callViewModel.startCall(userId, CallType.VIDEO)
                                navController.navigate("video_call/$userId")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Videocam, "Vidéo", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vidéo")
                        }
                    }
                }

                // Options
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // Liste des options
                item {
                    ContactDetailOption(
                        icon = Icons.Default.Image,
                        title = "Médias, liens et documents",
                        onClick = {
                            conversation?.let { conv ->
                                navController.navigate("conversation_media/${conv.id}")
                            }
                        }
                    )
                }

                item {
                    ContactDetailOption(
                        icon = Icons.Default.Star,
                        title = "Messages importants",
                        onClick = {
                            conversation?.let { conv ->
                                navController.navigate("starred_messages/${conv.id}")
                            }
                        }
                    )
                }

                item {
                    ContactDetailOption(
                        icon = Icons.Default.History,
                        title = "Historique des appels",
                        onClick = {
                            navController.navigate("call_history")
                        }
                    )
                }

                item {
                    ContactDetailOption(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        onClick = { }
                    )
                }

                item {
                    ContactDetailOption(
                        icon = Icons.Default.Block,
                        title = "Bloquer ce contact",
                        isDestructive = true,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactDetailOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
