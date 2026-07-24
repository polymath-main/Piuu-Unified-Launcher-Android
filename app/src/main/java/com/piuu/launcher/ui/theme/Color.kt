package com.piuu.launcher.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Global theme state for dynamic updates across all legacy references
var activePrimaryColor by mutableStateOf(Color(0xFF3B82F6))
var activeAccentColor by mutableStateOf(Color(0xFF8B5CF6))
var activeBgColor by mutableStateOf(Color(0xFF020817))

val LauncherBackground: Color get() = activeBgColor
val LauncherSurface: Color get() = activeBgColor.copy(alpha = 0.8f)
val LauncherGlass = Color(0x1AFFFFFF)
val LauncherBorder = Color(0x14FFFFFF)

val PrimaryBlue: Color get() = activePrimaryColor
val AccentPurple: Color get() = activeAccentColor
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val CardGlassBg = Color(0x261E293B)
val CardBorder = Color(0x3338BDF8)

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

