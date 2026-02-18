package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.PrivacySettings
import com.memoryshare.app.data.model.PrivacyVisibility
import com.memoryshare.app.ui.theme.*
import com.memoryshare.app.ui.viewmodel.PreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    preferencesViewModel: PreferencesViewModel,
    onBack: () -> Unit
) {
    val currentSettings by preferencesViewModel.privacySettings.collectAsState()

    // États locaux (modifiés localement avant sauvegarde)
    var showLastSeen by remember(currentSettings) { mutableStateOf(currentSettings.showLastSeen) }
    var showProfilePicture by remember(currentSettings) { mutableStateOf(currentSettings.showProfilePicture) }
    var showOnlineStatus by remember(currentSettings) { mutableStateOf(currentSettings.showOnlineStatus) }

    var showLastSeenDialog by remember { mutableStateOf(false) }
    var showProfilePicDialog by remember { mutableStateOf(false) }
    var showOnlineStatusDialog by remember { mutableStateOf(false) }

    fun saveAll() {
        preferencesViewModel.savePrivacySettings(
            PrivacySettings(
                showLastSeen = showLastSeen,
                showProfilePicture = showProfilePicture,
                showOnlineStatus = showOnlineStatus
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Confidentialité",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Qui peut voir mes informations",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CoralPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column {
                    PrivacyOptionItem(
                        icon = Icons.Outlined.AccessTime,
                        title = "Vu à (Last Seen)",
                        subtitle = showLastSeen.toLabel(),
                        onClick = { showLastSeenDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    PrivacyOptionItem(
                        icon = Icons.Outlined.AccountCircle,
                        title = "Photo de profil",
                        subtitle = showProfilePicture.toLabel(),
                        onClick = { showProfilePicDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    PrivacyOptionItem(
                        icon = Icons.Outlined.Circle,
                        title = "Statut en ligne",
                        subtitle = showOnlineStatus.toLabel(),
                        onClick = { showOnlineStatusDialog = true }
                    )
                }
            }

            Text(
                text = "Ces paramètres s'appliquent à tous les utilisateurs. " +
                        "Masquer votre «\u00a0Vu à\u00a0» vous empêche de voir celui des autres.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { saveAll(); onBack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Enregistrer", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogs de sélection
    if (showLastSeenDialog) {
        PrivacyVisibilityDialog(
            title = "Vu à (Last Seen)",
            current = showLastSeen,
            onSelect = { showLastSeen = it; showLastSeenDialog = false },
            onDismiss = { showLastSeenDialog = false }
        )
    }
    if (showProfilePicDialog) {
        PrivacyVisibilityDialog(
            title = "Photo de profil",
            current = showProfilePicture,
            onSelect = { showProfilePicture = it; showProfilePicDialog = false },
            onDismiss = { showProfilePicDialog = false }
        )
    }
    if (showOnlineStatusDialog) {
        PrivacyVisibilityDialog(
            title = "Statut en ligne",
            current = showOnlineStatus,
            onSelect = { showOnlineStatus = it; showOnlineStatusDialog = false },
            onDismiss = { showOnlineStatusDialog = false }
        )
    }
}

@Composable
private fun PrivacyOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CoralPrimary.copy(alpha = 0.1f), VioletPrimary.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = CoralPrimary, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun PrivacyVisibilityDialog(
    title: String,
    current: PrivacyVisibility,
    onSelect: (PrivacyVisibility) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                PrivacyVisibility.entries.forEach { visibility ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selected = visibility }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == visibility,
                            onClick = { selected = visibility },
                            colors = RadioButtonDefaults.colors(selectedColor = CoralPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(visibility.toLabel(), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = visibility.toDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text("Enregistrer", color = CoralPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

private fun PrivacyVisibility.toLabel(): String = when (this) {
    PrivacyVisibility.EVERYONE -> "Tout le monde"
    PrivacyVisibility.CONTACTS -> "Mes contacts"
    PrivacyVisibility.NOBODY -> "Personne"
}

private fun PrivacyVisibility.toDescription(): String = when (this) {
    PrivacyVisibility.EVERYONE -> "Visible par tous les utilisateurs"
    PrivacyVisibility.CONTACTS -> "Visible uniquement par vos contacts"
    PrivacyVisibility.NOBODY -> "Masqué pour tout le monde"
}
