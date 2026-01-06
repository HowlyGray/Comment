package com.memoryshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.memoryshare.app.data.model.CallType
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.CallViewModel
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact") },
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UserAvatar(
                            user = contact,
                            size = 120.dp
                        )

                        Text(
                            text = contact.displayName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "@${contact.displayName.lowercase().replace(" ", "")}",
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

                // Boutons d'action
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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

                // Bouton Message
                item {
                    Button(
                        onClick = {
                            // Créer ou trouver la conversation
                            currentUser?.let { current ->
                                // TODO: Naviguer vers la conversation
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Message, "Message", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Envoyer un message")
                    }
                }

                // Options
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // Liste des options
                item {
                    ContactDetailOption(
                        icon = Icons.Default.Image,
                        title = "Médias, liens et documents",
                        onClick = {
                            // TODO: Naviguer vers l'écran des médias
                        }
                    )
                }

                item {
                    ContactDetailOption(
                        icon = Icons.Default.Star,
                        title = "Messages importants",
                        onClick = {
                            // TODO: Naviguer vers les messages starred
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
                        onClick = {
                            // TODO: Gérer les notifications
                        }
                    )
                }

                item {
                    ContactDetailOption(
                        icon = Icons.Default.Block,
                        title = "Bloquer ce contact",
                        isDestructive = true,
                        onClick = {
                            // TODO: Bloquer le contact
                        }
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
