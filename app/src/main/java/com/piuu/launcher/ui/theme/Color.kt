package com.piuu.launcher.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch

// Global theme state for dynamic updates across all legacy references
var isDarkMode by mutableStateOf(true)
var activePrimaryColor by mutableStateOf(Color(0xFF3B82F6))
var activeAccentColor by mutableStateOf(Color(0xFF8B5CF6))
var activeBgColor by mutableStateOf(Color(0xFF020817))

val LauncherBackground: Color get() = if (isDarkMode) activeBgColor else Color(0xFFF1F5F9)
val LauncherSurface: Color get() = if (isDarkMode) activeBgColor.copy(alpha = 0.8f) else Color(0xFFFFFFFF)
val LauncherGlass: Color get() = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1A000000)
val LauncherBorder: Color get() = if (isDarkMode) Color(0x14FFFFFF) else Color(0x1F000000)

val PrimaryBlue: Color get() = activePrimaryColor
val AccentPurple: Color get() = activeAccentColor
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)

val NeonCyan = Color(0xFF06B6D4)
val NeonMagenta = Color(0xFFEC4899)
val NeonLime = Color(0xFF84CC16)
val SoftIndigo = Color(0xFF6366F1)

val TextPrimary: Color get() = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
val TextSecondary: Color get() = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
val TextMuted: Color get() = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)

val CardGlassBg: Color get() = if (isDarkMode) Color(0x261E293B) else Color(0xF2FFFFFF)
val CardBorder: Color get() = if (isDarkMode) Color(0x3338BDF8) else Color(0x333B82F6)

val GlassDockSurface: Color get() = if (isDarkMode) Color(0x4D0F172A) else Color(0xD9FFFFFF)
val GlassDockBorder: Color get() = if (isDarkMode) Color(0x3364748B) else Color(0x33CBD5E1)

val PillSelectedBg: Color get() = activePrimaryColor.copy(alpha = 0.25f)
val PillUnselectedBg: Color get() = if (isDarkMode) Color(0x1F64748B) else Color(0x1FE2E8F0)

// Parsing utilities
fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val clean = hex.removePrefix("#").trim()
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else if (clean.length == 8) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}

fun parseRgbaOrHexColor(colorStr: String, defaultColor: Color): Color {
    val clean = colorStr.trim()
    if (clean.startsWith("#")) {
        return parseHexColor(clean, defaultColor)
    }
    if (clean.startsWith("rgba") || clean.startsWith("rgb")) {
        try {
            val content = clean.substringAfter("(").substringBefore(")")
            val parts = content.split(",").map { it.trim() }
            if (parts.size >= 3) {
                val r = parts[0].toInt().coerceIn(0, 255)
                val g = parts[1].toInt().coerceIn(0, 255)
                val b = parts[2].toInt().coerceIn(0, 255)
                val alpha = if (parts.size >= 4) parts[3].toFloat().coerceIn(0f, 1f) else 1f
                return Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = alpha)
            }
        } catch (_: Exception) {}
    }
    return defaultColor
}

@Composable
fun Modifier.longPressPulseEffect(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }

    return this
        .graphicsLayer(
            scaleX = scale.value,
            scaleY = scale.value
        )
        .pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onPress = {
                    try {
                        scale.animateTo(0.88f, animationSpec = androidx.compose.animation.core.tween(120))
                        val released = tryAwaitRelease()
                        if (released) {
                            scale.animateTo(1.06f, animationSpec = androidx.compose.animation.core.tween(80))
                            scale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(80))
                            onClick()
                        } else {
                            scale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(120))
                        }
                    } catch (e: Exception) {
                        scale.snapTo(1f)
                    }
                },
                onLongPress = {
                    coroutineScope.launch {
                        scale.animateTo(1.22f, animationSpec = androidx.compose.animation.core.tween(100))
                        scale.animateTo(0.94f, animationSpec = androidx.compose.animation.core.tween(80))
                        scale.animateTo(1.05f, animationSpec = androidx.compose.animation.core.tween(80))
                        scale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(100))
                        onLongClick()
                    }
                }
            )
        }
}

