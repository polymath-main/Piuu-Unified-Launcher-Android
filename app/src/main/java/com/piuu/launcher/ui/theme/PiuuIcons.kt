package com.piuu.launcher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern vector icons and glow badges for Piuu built-in tools.
 */
object PiuuIcons {

    @Composable
    fun ToolIcon(
        icon: ImageVector,
        primaryGradient: List<Color>,
        modifier: Modifier = Modifier,
        size: Dp = 48.dp,
        iconTint: Color = Color.White
    ) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(primaryGradient))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }

    @Composable
    fun SettingsIcon(modifier: Modifier = Modifier, size: Dp = 48.dp) {
        ToolIcon(
            icon = Icons.Default.Settings,
            primaryGradient = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
            modifier = modifier,
            size = size
        )
    }

    @Composable
    fun NotesIcon(modifier: Modifier = Modifier, size: Dp = 48.dp) {
        ToolIcon(
            icon = Icons.Default.EditNote,
            primaryGradient = listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
            modifier = modifier,
            size = size
        )
    }

    @Composable
    fun PipIcon(modifier: Modifier = Modifier, size: Dp = 48.dp) {
        ToolIcon(
            icon = Icons.Default.PictureInPictureAlt,
            primaryGradient = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
            modifier = modifier,
            size = size
        )
    }

    @Composable
    fun MarketplaceIcon(modifier: Modifier = Modifier, size: Dp = 48.dp) {
        ToolIcon(
            icon = Icons.Default.Storefront,
            primaryGradient = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
            modifier = modifier,
            size = size
        )
    }

    @Composable
    fun SuiteIcon(modifier: Modifier = Modifier, size: Dp = 48.dp) {
        ToolIcon(
            icon = Icons.Default.Widgets,
            primaryGradient = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
            modifier = modifier,
            size = size
        )
    }
}
