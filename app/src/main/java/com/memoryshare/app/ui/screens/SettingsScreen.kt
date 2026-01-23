package com.memoryshare.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.PostVisibility
import com.memoryshare.app.ui.viewmodel.PreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesViewModel: PreferencesViewModel,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val fabOnLeft by preferencesViewModel.fabOnLeft.collectAsState()
    val defaultPostVisibility by preferencesViewModel.defaultPostVisibility.collectAsState()
    var showVisibilityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Section Compte
            Text(
                text = "Compte",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            SettingsMenuItem(
                icon = Icons.Default.Person,
                title = "Mon profil",
                onClick = onNavigateToProfile
            )

            SettingsMenuItem(
                icon = Icons.Default.Email,
                title = "Email",
                subtitle = "Gérer votre adresse email",
                onClick = {}
            )

            Divider()

            // Section Préférences
            Text(
                text = "Préférences",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            SettingsMenuItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Gérer les notifications",
                onClick = {}
            )

            SettingsMenuItem(
                icon = Icons.Default.Lock,
                title = "Confidentialité",
                subtitle = "Contrôlez qui peut voir vos contenus",
                onClick = {}
            )

            ListItem(
                headlineContent = {
                    Text("Visibilité par défaut des publications")
                },
                supportingContent = {
                    Text(
                        text = when (defaultPostVisibility) {
                            PostVisibility.PUBLIC -> "Public"
                            PostVisibility.FRIENDS -> "Amis uniquement"
                            PostVisibility.FOLLOWERS -> "Followers uniquement"
                            PostVisibility.FRIENDS_AND_FOLLOWERS -> "Amis et followers"
                            PostVisibility.PRIVATE -> "Privé"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Visibilité"
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { showVisibilityDialog = true }
            )

            Divider()

            // Section Accessibilité
            Text(
                text = "Accessibilité",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            ListItem(
                headlineContent = {
                    Text("Boutons pour gauchers")
                },
                supportingContent = {
                    Text(
                        text = "Placer les boutons d'action à gauche",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Accessible,
                        contentDescription = "Accessibilité"
                    )
                },
                trailingContent = {
                    Switch(
                        checked = fabOnLeft,
                        onCheckedChange = { preferencesViewModel.setFabOnLeft(it) }
                    )
                }
            )

            Divider()

            // Section Aide et support
            Text(
                text = "Aide et support",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            SettingsMenuItem(
                icon = Icons.Default.Help,
                title = "Centre d'aide",
                onClick = {}
            )

            SettingsMenuItem(
                icon = Icons.Default.Info,
                title = "À propos",
                subtitle = "Version 1.0.0",
                onClick = {}
            )

            Divider()

            // Déconnexion
            SettingsMenuItem(
                icon = Icons.Default.Logout,
                title = "Se déconnecter",
                onClick = onLogout,
                textColor = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog pour changer la visibilité par défaut
    if (showVisibilityDialog) {
        var selectedVisibility by remember { mutableStateOf(defaultPostVisibility) }
        AlertDialog(
            onDismissRequest = { showVisibilityDialog = false },
            title = { Text("Visibilité par défaut") },
            text = {
                Column {
                    Text(
                        text = "Choisissez la visibilité par défaut de vos nouvelles publications",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    PostVisibility.values().forEach { visibility ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVisibility = visibility }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVisibility == visibility,
                                onClick = { selectedVisibility = visibility }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (visibility) {
                                        PostVisibility.PUBLIC -> "Public"
                                        PostVisibility.FRIENDS -> "Amis uniquement"
                                        PostVisibility.FOLLOWERS -> "Followers uniquement"
                                        PostVisibility.FRIENDS_AND_FOLLOWERS -> "Amis et followers"
                                        PostVisibility.PRIVATE -> "Privé"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (visibility) {
                                        PostVisibility.PUBLIC -> "Visible par tout le monde"
                                        PostVisibility.FRIENDS -> "Visible uniquement par vos amis"
                                        PostVisibility.FOLLOWERS -> "Visible uniquement par vos followers"
                                        PostVisibility.FRIENDS_AND_FOLLOWERS -> "Visible par vos amis et followers"
                                        PostVisibility.PRIVATE -> "Visible uniquement par vous"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferencesViewModel.setDefaultPostVisibility(selectedVisibility)
                        showVisibilityDialog = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVisibilityDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = textColor
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
