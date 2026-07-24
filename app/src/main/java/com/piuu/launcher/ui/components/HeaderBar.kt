package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.SystemMetrics
import com.piuu.launcher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HeaderBar(
    metrics: SystemMetrics,
    onOpenSearch: () -> Unit,
    onOpenBrainChat: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenMetrics: () -> Unit,
    onOpenSchemaEditor: () -> Unit,
    onOpenWebView: () -> Unit = {}
) {
    val currentTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Time & Carrier / Launcher title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentTime,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(TextMuted)
                )
                Text(
                    text = "5G • Piuu Launcher",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Right: Network, Battery, Notifications
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "WiFi",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (metrics.battery_pct > 80) Icons.Default.BatteryFull
                        else if (metrics.battery_pct > 30) Icons.Default.Battery4Bar
                        else Icons.Default.Battery1Bar,
                        contentDescription = "Battery",
                        tint = if (metrics.battery_pct < 20) DangerRed else SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${metrics.battery_pct}%",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Launcher Toolbar Row (Search, Brain AI, Marketplace, Metrics, Schema Editor, Quick Settings pull handle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LauncherGlass)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search field button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Search apps, actions or AI...",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Quick Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brain AI
                IconButton(onClick = onOpenBrainChat, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Brain AI",
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Marketplace
                IconButton(onClick = onOpenMarketplace, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Marketplace",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Metrics Dashboard
                IconButton(onClick = onOpenMetrics, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Metrics",
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Schema Editor IDE
                IconButton(onClick = onOpenSchemaEditor, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Schema IDE",
                        tint = WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Hybrid WebView
                IconButton(onClick = onOpenWebView, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "WebView Drawer",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
