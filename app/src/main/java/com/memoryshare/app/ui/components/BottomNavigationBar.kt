package com.memoryshare.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.memoryshare.app.ui.theme.*

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
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
    val items = listOf(
        BottomNavItem("Messages", Icons.Filled.Chat, Icons.Outlined.Chat, onNavigateToMessages),
        BottomNavItem("Actualités", Icons.Filled.Explore, Icons.Outlined.Explore, onNavigateToFeed),
        BottomNavItem("Souvenirs", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, onNavigateToMemories),
        BottomNavItem("Profil", Icons.Filled.Person, Icons.Outlined.Person, onNavigateToProfile)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
    ) {
        Column {
            // Gradient accent line at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                CoralPrimary.copy(alpha = 0.3f),
                                VioletPrimary.copy(alpha = 0.3f),
                                CoralPrimary.copy(alpha = 0.3f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "scale"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "iconColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = item.onClick
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Gradient pill background for selected item
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    CoralPrimary.copy(alpha = 0.15f),
                                                    VioletPrimary.copy(alpha = 0.15f)
                                                )
                                            )
                                        )
                                )
                            }
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(scale)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor
                        )
                    }
                }
            }
        }
    }
}
