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
    onOpenWebView: () -> Unit = {},
    onOpenQuickSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Launcher Toolbar Row (Search Icon Only, Brain AI, Marketplace, Metrics, Schema Editor, Quick Settings pull handle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LauncherGlass)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Icon Only
            IconButton(onClick = onOpenSearch, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

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

                // Quick Settings / Control Center
                IconButton(onClick = onOpenQuickSettings, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Control Center",
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
