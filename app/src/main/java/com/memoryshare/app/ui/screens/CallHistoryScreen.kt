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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.memoryshare.app.data.model.Call
import com.memoryshare.app.data.model.CallStatus
import com.memoryshare.app.data.model.CallType
import com.memoryshare.app.ui.components.UserAvatar
import com.memoryshare.app.ui.viewmodel.CallViewModel
import com.memoryshare.app.ui.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    navController: NavController,
    callViewModel: CallViewModel,
    userViewModel: UserViewModel
) {
    val calls by callViewModel.calls.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des appels") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // TODO: Afficher options (effacer tout, etc.)
                    }) {
                        Icon(Icons.Default.MoreVert, "Plus d'options")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (calls.isEmpty()) {
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
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Aucun appel",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Votre historique d'appels apparaîtra ici",
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
                items(calls) { call ->
                    CallHistoryItem(
                        call = call,
                        userViewModel = userViewModel,
                        onCallClick = {
                            // Rappeler ce contact
                            callViewModel.startCall(call.userId, call.type)
                            val route = if (call.type == CallType.VIDEO) {
                                "video_call/${call.userId}"
                            } else {
                                "call/${call.userId}"
                            }
                            navController.navigate(route)
                        },
                        onDeleteClick = {
                            callViewModel.deleteCall(call)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(
    call: Call,
    userViewModel: UserViewModel,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val user by userViewModel.getUserById(call.userId).collectAsState(initial = null)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCallClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        user?.let { currentUser ->
            UserAvatar(user = currentUser, size = 48.dp)
        } ?: Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Informations de l'appel
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user?.name ?: "Utilisateur",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Icône selon le type et statut d'appel
                Icon(
                    imageVector = when {
                        call.status == CallStatus.MISSED -> Icons.Default.CallMissed
                        call.isIncoming -> Icons.Default.CallReceived
                        else -> Icons.Default.CallMade
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (call.status) {
                        CallStatus.MISSED -> Color.Red
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Type d'appel et durée
                Text(
                    text = buildString {
                        append(if (call.type == CallType.VIDEO) "Vidéo" else "Vocal")
                        call.duration?.let { duration ->
                            append(" • ${formatDuration(duration)}")
                        }
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatCallTime(call.timestamp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bouton pour rappeler
        IconButton(
            onClick = onCallClick
        ) {
            Icon(
                imageVector = if (call.type == CallType.VIDEO) Icons.Default.Videocam
                             else Icons.Default.Call,
                contentDescription = "Rappeler",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Bouton info/supprimer
        IconButton(
            onClick = onDeleteClick
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Supprimer",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format("%d:%02d", minutes, secs)
        else -> "${secs}s"
    }
}

private fun formatCallTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "À l'instant"
        diff < 3600000 -> "${diff / 60000} min"
        diff < 86400000 -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Aujourd'hui à ${sdf.format(Date(timestamp))}"
        }
        diff < 172800000 -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Hier à ${sdf.format(Date(timestamp))}"
        }
        diff < 604800000 -> {
            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
