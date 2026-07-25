package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.NoteItem
import com.piuu.launcher.model.SystemApp
import com.piuu.launcher.model.SystemMetrics
import com.piuu.launcher.model.StyleProps
import com.piuu.launcher.model.LauncherElement
import com.piuu.launcher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockWidgetComposable(
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.US).format(Date())
    val timeStr = SimpleDateFormat("hh:mm", Locale.US).format(Date())
    val amPmStr = SimpleDateFormat("a", Locale.US).format(Date())

    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, PrimaryBlue) else PrimaryBlue
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeStr,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = if (!title.isNullOrBlank()) "$title • $dateStr" else dateStr,
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WeatherWidgetComposable(
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, WarningAmber) else WarningAmber
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = DangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (!title.isNullOrBlank()) title else "San Francisco", fontSize = 13.sp, color = TextSecondary)
                }
                Text(
                    text = "24°C",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(text = "Sunny • H: 26° L: 17°", fontSize = 12.sp, color = TextMuted)
            }
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "Sunny",
                tint = accentColor,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun BatteryWidgetComposable(
    metrics: SystemMetrics,
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, SuccessGreen) else SuccessGreen
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = if (!title.isNullOrBlank()) title else "Battery Health", fontSize = 12.sp, color = TextMuted)
                Text("${metrics.battery_pct}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accentColor)
                Text("Est. 14h 20m • ${metrics.battery_temp}°C", fontSize = 11.sp, color = TextSecondary)
            }
            CircularProgressIndicator(
                progress = metrics.battery_pct / 100f,
                color = accentColor,
                trackColor = Color(0x33FFFFFF),
                strokeWidth = 6.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun SystemStatsWidgetComposable(
    metrics: SystemMetrics,
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, AccentPurple) else AccentPurple
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (!title.isNullOrBlank()) title else "System Monitor", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = accentColor)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CPU Load: ${metrics.cpu_usage}%", fontSize = 12.sp, color = TextSecondary)
                    LinearProgressIndicator(
                        progress = metrics.cpu_usage / 100f,
                        color = PrimaryBlue,
                        trackColor = Color(0x33FFFFFF),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("RAM: ${metrics.ram_used_mb} GB", fontSize = 12.sp, color = TextSecondary)
                    LinearProgressIndicator(
                        progress = metrics.ram_used_mb / metrics.ram_total_mb,
                        color = accentColor,
                        trackColor = Color(0x33FFFFFF),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun MediaPlayerWidgetComposable(
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    var isPlaying by remember { mutableStateOf(true) }
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, AccentPurple) else AccentPurple
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (!title.isNullOrBlank()) title else "Midnight City", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("M83 • Spotify", fontSize = 12.sp, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun AiInsightWidgetComposable(
    onOpenBrainChat: () -> Unit,
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, AccentPurple) else AccentPurple
    val bgOpacity = styleProps.opacity ?: 0.2f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
            .clickable { onOpenBrainChat() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (!title.isNullOrBlank()) title else "Piuu AI Insight", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(containerColor = accentColor) { Text("AI", color = Color.White) }
                }
                Text(
                    text = "High productivity morning! You have 2 calendar events and 3 unread messages.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
fun QuickNotesWidgetComposable(
    notes: List<NoteItem>,
    onAddNote: (String) -> Unit,
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Quick Note", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    placeholder = { Text("Type note...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNoteText.isNotBlank()) {
                        onAddNote(newNoteText)
                        newNoteText = ""
                        showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, PrimaryBlue) else PrimaryBlue
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (!title.isNullOrBlank()) title else "Quick Notepad", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note", tint = accentColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            notes.take(2).forEach { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(WarningAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.content,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PiuuSuiteFolderWidgetComposable(
    onOpenFolder: () -> Unit,
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, SuccessGreen) else SuccessGreen
    val bgOpacity = styleProps.opacity ?: 0.2f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
            .clickable { onOpenFolder() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (!title.isNullOrBlank()) title else "Piuu Suite Apps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Piuu Brain • IDE • Notes • Storage Sync", fontSize = 12.sp, color = TextSecondary)
            }
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = accentColor)
        }
    }
}

@Composable
fun PredictiveSuggestionsWidgetComposable(
    apps: List<SystemApp>,
    onLaunchApp: (SystemApp) -> Unit,
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, PrimaryBlue) else PrimaryBlue
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = if (!title.isNullOrBlank()) title else "Predictive Apps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(apps.take(5)) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onLaunchApp(app) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            com.piuu.launcher.repository.AppIconImage(
                                packageName = app.package_name,
                                modifier = Modifier.size(20.dp),
                                fallbackIconName = app.icon_name,
                                tintColor = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = app.name, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCenterWidgetComposable(
    notifications: List<com.piuu.launcher.model.NotificationItem>,
    onDismissNotification: (String) -> Unit = {},
    onClearAll: () -> Unit = {},
    title: String? = null,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, PrimaryBlue) else PrimaryBlue
    val bgOpacity = styleProps.opacity ?: 0.15f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(text = if (!title.isNullOrBlank()) title else "Notification Center", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (notifications.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${notifications.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAll, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Clear All", fontSize = 11.sp, color = DangerRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active notifications", fontSize = 12.sp, color = TextMuted)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notifications.take(3).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(LauncherGlass)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(item.icon_name) {
                                        "brain" -> Icons.Default.Psychology
                                        "message" -> Icons.Default.Chat
                                        else -> Icons.Default.Notifications
                                    },
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.app_name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                    Text(item.time, fontSize = 10.sp, color = TextMuted)
                                }
                                Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    item.message,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDismissNotification(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
