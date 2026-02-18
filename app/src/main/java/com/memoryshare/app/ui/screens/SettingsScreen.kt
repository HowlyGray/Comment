package com.memoryshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.memoryshare.app.data.model.PostVisibility
import com.memoryshare.app.ui.theme.*
import com.memoryshare.app.ui.viewmodel.PreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesViewModel: PreferencesViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val fabOnLeft by preferencesViewModel.fabOnLeft.collectAsState()
    val defaultPostVisibility by preferencesViewModel.defaultPostVisibility.collectAsState()
    var showVisibilityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Paramètres",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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

            // Section Compte
            Text(
                text = "Compte",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CoralPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                Column {
                    SettingsMenuItem(
                        icon = Icons.Outlined.Person,
                        title = "Mon profil",
                        onClick = onNavigateToProfile
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsMenuItem(
                        icon = Icons.Outlined.Email,
                        title = "Email",
                        subtitle = "Gérer votre adresse email",
                        onClick = {}
                    )
                }
            }

            // Section Préférences
            Text(
                text = "Préférences",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CoralPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                Column {
                    SettingsMenuItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = "Gérer les notifications",
                        onClick = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsMenuItem(
                        icon = Icons.Outlined.Lock,
                        title = "Confidentialité",
                        subtitle = "Contrôlez qui peut voir vos contenus",
                        onClick = onNavigateToPrivacy
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                CoralPrimary.copy(alpha = 0.1f),
                                                VioletPrimary.copy(alpha = 0.1f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = "Visibilité",
                                    modifier = Modifier.size(20.dp),
                                    tint = CoralPrimary
                                )
                            }
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
                }
            }

            // Section Accessibilité
            Text(
                text = "Accessibilité",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CoralPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            CoralPrimary.copy(alpha = 0.1f),
                                            VioletPrimary.copy(alpha = 0.1f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Accessible,
                                contentDescription = "Accessibilité",
                                modifier = Modifier.size(20.dp),
                                tint = CoralPrimary
                            )
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = fabOnLeft,
                            onCheckedChange = { preferencesViewModel.setFabOnLeft(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CoralPrimary
                            )
                        )
                    }
                )
            }

            // Section Aide et support
            Text(
                text = "Aide et support",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CoralPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                Column {
                    SettingsMenuItem(
                        icon = Icons.Outlined.Help,
                        title = "Centre d'aide",
                        onClick = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsMenuItem(
                        icon = Icons.Outlined.Info,
                        title = "À propos",
                        subtitle = "Version 1.0.0",
                        onClick = {}
                    )
                }
            }

            // Déconnexion
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                SettingsMenuItem(
                    icon = Icons.Outlined.Logout,
                    title = "Se déconnecter",
                    onClick = onLogout,
                    textColor = ErrorRose
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showVisibilityDialog) {
        var selectedVisibility by remember { mutableStateOf(defaultPostVisibility) }
        AlertDialog(
            onDismissRequest = { showVisibilityDialog = false },
            title = { Text("Visibilité par défaut", fontWeight = FontWeight.Bold) },
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
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedVisibility = visibility }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVisibility == visibility,
                                onClick = { selectedVisibility = visibility },
                                colors = RadioButtonDefaults.colors(selectedColor = CoralPrimary)
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
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        preferencesViewModel.setDefaultPostVisibility(selectedVisibility)
                        showVisibilityDialog = false
                    }
                ) {
                    Text("Enregistrer", color = CoralPrimary, fontWeight = FontWeight.SemiBold)
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
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = { Text(text = title, color = textColor) },
        supportingContent = subtitle?.let {
            { Text(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (textColor == ErrorRose) {
                                listOf(ErrorRose.copy(alpha = 0.1f), ErrorRose.copy(alpha = 0.1f))
                            } else {
                                listOf(
                                    CoralPrimary.copy(alpha = 0.1f),
                                    VioletPrimary.copy(alpha = 0.1f)
                                )
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textColor.copy(alpha = if (textColor == ErrorRose) 1f else 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
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
