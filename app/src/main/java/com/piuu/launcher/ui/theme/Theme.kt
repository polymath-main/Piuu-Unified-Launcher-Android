package com.piuu.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.piuu.launcher.model.LauncherTheme

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentPurple,
    tertiary = SuccessGreen,
    background = LauncherBackground,
    surface = LauncherSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PiuuLauncherTheme(
    theme: LauncherTheme? = null,
    content: @Composable () -> Unit
) {
    LaunchedEffect(theme) {
        theme?.let {
            activePrimaryColor = parseHexColor(it.primary_color, Color(0xFF3B82F6))
            activeAccentColor = parseHexColor(it.accent_color, Color(0xFF8B5CF6))
            activeBgColor = parseRgbaOrHexColor(it.bg_overlay, Color(0xFF020817))
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

