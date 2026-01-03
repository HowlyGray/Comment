package com.memoryshare.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onNavigateToMessages: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar {
        val items = listOf(
            BottomNavItem("Messages", Icons.Default.Chat, onNavigateToMessages),
            BottomNavItem("Actualités", Icons.Default.Home, onNavigateToFeed),
            BottomNavItem("Souvenirs", Icons.Default.PhotoLibrary, onNavigateToMemories),
            BottomNavItem("Profil", Icons.Default.Person, onNavigateToProfile)
        )

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selectedTab == index,
                onClick = item.onClick
            )
        }
    }
}
