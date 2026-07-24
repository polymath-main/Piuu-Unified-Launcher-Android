package com.piuu.launcher.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.NotificationItem
import com.piuu.launcher.model.SystemMetrics
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsShade(
    visible: Boolean,
    metrics: SystemMetrics,
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleBatterySaver: () -> Unit,
    onToggleDnd: () -> Unit,
    onClearNotifications: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LauncherBackground,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Control Center & Notifications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Toggles Grid (2x3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickToggleTile(
                        title = "Wi-Fi",
                        subtitle = if (metrics.wifi_enabled) "Connected (5G)" else "Off",
                        icon = Icons.Default.Wifi,
                        active = metrics.wifi_enabled,
                        activeColor = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleWifi
                    )
                    QuickToggleTile(
                        title = "Bluetooth",
                        subtitle = if (metrics.bluetooth_enabled) "On (Headphones)" else "Off",
                        icon = Icons.Default.Bluetooth,
                        active = metrics.bluetooth_enabled,
                        activeColor = AccentPurple,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleBluetooth
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickToggleTile(
                        title = "Flashlight",
                        subtitle = if (metrics.flashlight_enabled) "On" else "Off",
                        icon = Icons.Default.FlashOn,
                        active = metrics.flashlight_enabled,
                        activeColor = WarningAmber,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleFlashlight
                    )
                    QuickToggleTile(
                        title = "Battery Saver",
                        subtitle = if (metrics.battery_saver) "Active" else "Standard",
                        icon = Icons.Default.BatterySaver,
                        active = metrics.battery_saver,
                        activeColor = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleBatterySaver
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickToggleTile(
                        title = "Do Not Disturb",
                        subtitle = if (metrics.do_not_disturb) "Silent" else "Off",
                        icon = Icons.Default.DoNotDisturbOn,
                        active = metrics.do_not_disturb,
                        activeColor = DangerRed,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleDnd
                    )
                    QuickToggleTile(
                        title = "Location",
                        subtitle = "High Accuracy",
                        icon = Icons.Default.LocationOn,
                        active = true,
                        activeColor = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // System Metrics Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live System Health", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem(title = "CPU", value = "${metrics.cpu_usage}%", color = PrimaryBlue)
                        MetricItem(title = "RAM", value = "${metrics.ram_used_mb} GB / ${metrics.ram_total_mb} GB", color = AccentPurple)
                        MetricItem(title = "Battery", value = "${metrics.battery_pct}%", color = SuccessGreen)
                        MetricItem(title = "Network", value = "${(metrics.network_speed_kbps / 1000).toInt()} MB/s", color = WarningAmber)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications (${notifications.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (notifications.isNotEmpty()) {
                    TextTextButton(text = "Clear All", onClick = onClearNotifications)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No new notifications", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    items(notifications) { notif ->
                        NotificationCardItem(item = notif)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuickToggleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (active) activeColor.copy(alpha = 0.25f) else CardGlassBg
    val border = if (active) activeColor else LauncherBorder
    val iconTint = if (active) activeColor else TextMuted

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (active) Color.White else iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun MetricItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = title, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
fun NotificationCardItem(item: NotificationItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(item.icon_name) {
                        "brain" -> Icons.Default.AutoAwesome
                        "message" -> Icons.Default.Message
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = item.app_name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(text = item.time, fontSize = 10.sp, color = TextMuted)
                }
                Text(text = item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = item.message, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun TextTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryBlue,
        modifier = Modifier.clickable { onClick() }
    )
}
