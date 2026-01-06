package com.memoryshare.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageType
import com.memoryshare.app.ui.components.MediaItem
import com.memoryshare.app.ui.viewmodel.MessageViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationMediaScreen(
    conversationId: String,
    navController: NavController,
    messageViewModel: MessageViewModel
) {
    val messages by messageViewModel.currentMessages.collectAsState()

    // Filtrer les messages par type
    val mediaMessages = messages.filter {
        it.type == MessageType.IMAGE || it.type == MessageType.VIDEO
    }
    val fileMessages = messages.filter { it.type == MessageType.FILE }
    val linkMessages = messages.filter {
        it.type == MessageType.TEXT && containsLink(it.content)
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Médias", "Documents", "Liens")

    LaunchedEffect(conversationId) {
        messageViewModel.loadMessages(conversationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Médias et documents") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Contenu selon l'onglet sélectionné
            when (selectedTab) {
                0 -> MediaGrid(mediaMessages)
                1 -> DocumentsList(fileMessages, navController)
                2 -> LinksList(linkMessages, navController)
            }
        }
    }
}

@Composable
fun MediaGrid(messages: List<Message>) {
    if (messages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Aucun média",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { message ->
                MediaItem(
                    mediaUrl = message.mediaUrl ?: "",
                    isVideo = message.type == MessageType.VIDEO,
                    onClick = {
                        // TODO: Ouvrir le média en plein écran
                    }
                )
            }
        }
    }
}

@Composable
fun DocumentsList(messages: List<Message>, navController: NavController) {
    if (messages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Aucun document",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                DocumentItem(message)
            }
        }
    }
}

@Composable
fun DocumentItem(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = message.content,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(message.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* TODO: Télécharger */ }) {
                Icon(Icons.Default.Download, "Télécharger")
            }
        }
    }
}

@Composable
fun LinksList(messages: List<Message>, navController: NavController) {
    if (messages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Aucun lien",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                LinkItem(message)
            }
        }
    }
}

@Composable
fun LinkItem(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = extractLink(message.content),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(message.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* TODO: Ouvrir le lien */ }) {
                Icon(Icons.Default.OpenInNew, "Ouvrir")
            }
        }
    }
}

private fun containsLink(text: String): Boolean {
    return text.contains("http://") || text.contains("https://") || text.contains("www.")
}

private fun extractLink(text: String): String {
    val regex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+)".toRegex()
    return regex.find(text)?.value ?: text
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
