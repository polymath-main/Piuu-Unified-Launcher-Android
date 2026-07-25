package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.piuu.launcher.model.LauncherElement
import com.piuu.launcher.model.StyleProps
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizerModal(
    element: LauncherElement?,
    onDismiss: () -> Unit,
    onSaveElement: (LauncherElement) -> Unit
) {
    if (element == null) return

    var customTitle by remember(element) { mutableStateOf(element.title ?: "") }
    var widthSpan by remember(element) { mutableStateOf(element.style_props.w ?: 4) } // 2 = Compact (1 col), 4 = Wide (2 col)
    var borderRadius by remember(element) { mutableStateOf(element.style_props.borderRadius ?: 24) }
    var opacity by remember(element) { mutableStateOf(element.style_props.opacity ?: 0.15f) }
    var isBorderVisible by remember(element) { mutableStateOf(element.style_props.borderStyle != "none") }
    var selectedColorHex by remember(element) { mutableStateOf(element.style_props.accentColor ?: "#3B82F6") }

    val colorsList = listOf(
        "#3B82F6", // Primary Blue
        "#8B5CF6", // Accent Purple
        "#10B981", // Success Green
        "#F59E0B", // Warning Amber
        "#EF4444", // Danger Red
        "#EC4899", // Cyber Pink
        "#14B8A6", // Mint
        "#64748B", // Slate Grey
        "#FF7A00"  // Orange
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = activeBgColor.copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Customize Widget",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = element.type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x1AFFFFFF))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title Editor
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Widget Header Title",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            placeholder = { Text("E.g., My Weather, Workspace Clock...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                unfocusedBorderColor = LauncherBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = TextSecondary,
                                unfocusedLabelColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Grid Width (Span) Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Width Span (Columns)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { widthSpan = 2 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (widthSpan == 2) parseHexColor(selectedColorHex, PrimaryBlue) else Color(0x1AFFFFFF),
                                    contentColor = if (widthSpan == 2) Color.White else TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Compact (1 Col)")
                            }
                            Button(
                                onClick = { widthSpan = 4 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (widthSpan == 4) parseHexColor(selectedColorHex, PrimaryBlue) else Color(0x1AFFFFFF),
                                    contentColor = if (widthSpan == 4) Color.White else TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Wide (2 Cols)")
                            }
                        }
                    }

                    // Border Radius Editor
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Corner Roundness",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${borderRadius}dp",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = borderRadius.toFloat(),
                            onValueChange = { borderRadius = it.toInt() },
                            valueRange = 0f..40f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                activeTrackColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                inactiveTrackColor = LauncherBorder
                            )
                        )
                    }

                    // Background Opacity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Background Opacity",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${(opacity * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                activeTrackColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                inactiveTrackColor = LauncherBorder
                            )
                        )
                    }

                    // Border Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Widget Border",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Switch(
                            checked = isBorderVisible,
                            onCheckedChange = { isBorderVisible = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0x1AFFFFFF)
                            )
                        )
                    }

                    // Accent Colors Palette
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = parseHexColor(selectedColorHex, PrimaryBlue), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Accent Theme Color",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        // Horizontal Scrollable/Wrapped Color Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorsList.forEach { colorHex ->
                                val color = parseHexColor(colorHex, Color.Gray)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColorHex.equals(colorHex, ignoreCase = true)) 3.dp else 0.dp,
                                            color = if (selectedColorHex.equals(colorHex, ignoreCase = true)) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = colorHex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom Hex Input
                        OutlinedTextField(
                            value = selectedColorHex,
                            onValueChange = {
                                if (it.startsWith("#") && it.length <= 9) {
                                    selectedColorHex = it
                                } else if (!it.startsWith("#") && it.length <= 8) {
                                    selectedColorHex = "#$it"
                                }
                            },
                            label = { Text("Custom Hex Color Code") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                unfocusedBorderColor = LauncherBorder,
                                focusedLabelColor = parseHexColor(selectedColorHex, PrimaryBlue),
                                unfocusedLabelColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = borderStroke(1.dp, LauncherBorder)
                    ) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val updatedProps = element.style_props.copy(
                                w = widthSpan,
                                borderRadius = borderRadius,
                                opacity = opacity,
                                borderStyle = if (isBorderVisible) "solid" else "none",
                                accentColor = selectedColorHex
                            )
                            val updatedElement = element.copy(
                                title = customTitle.ifBlank { null },
                                style_props = updatedProps
                            )
                            onSaveElement(updatedElement)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = parseHexColor(selectedColorHex, PrimaryBlue),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Changes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Compact border Stroke helper
@Composable
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
