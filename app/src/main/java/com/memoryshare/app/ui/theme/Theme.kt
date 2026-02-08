package com.memoryshare.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = CoralDark,
    onPrimaryContainer = CoralLight,

    secondary = VioletPrimary,
    onSecondary = Color.White,
    secondaryContainer = VioletDark,
    onSecondaryContainer = VioletLight,

    tertiary = GradientSunset,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3D1A4D),
    onTertiaryContainer = Color(0xFFFFD6E8),

    background = DarkBackground,
    onBackground = TextPrimaryDark,

    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,

    surfaceContainerHighest = DarkSurfaceBright,
    surfaceContainerHigh = DarkSurfaceCard,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,

    error = ErrorRose,
    onError = Color.White,
    errorContainer = Color(0xFF3D1414),
    onErrorContainer = ErrorRoseLight,

    outline = Color(0xFF3A3A50),
    outlineVariant = Color(0xFF2A2A3E),

    inverseSurface = LightSurface,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = CoralDark,

    scrim = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0E0),
    onPrimaryContainer = CoralDark,

    secondary = VioletPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE5FF),
    onSecondaryContainer = VioletDark,

    tertiary = GradientSunset,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0F0),
    onTertiaryContainer = Color(0xFF4D1A30),

    background = LightBackground,
    onBackground = TextPrimaryLight,

    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,

    surfaceContainerHighest = Color(0xFFECE8F0),
    surfaceContainerHigh = Color(0xFFF2EEF6),
    surfaceContainerLow = Color(0xFFFAF8FC),
    surfaceContainer = Color(0xFFF5F2FA),

    error = ErrorRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF8B1A1A),

    outline = Color(0xFFD8D0E0),
    outlineVariant = Color(0xFFE8E2EE),

    inverseSurface = DarkSurface,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = CoralLight,

    scrim = Color(0x66000000)
)

@Composable
fun MemoryShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce our brand palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Transparent status bar for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
