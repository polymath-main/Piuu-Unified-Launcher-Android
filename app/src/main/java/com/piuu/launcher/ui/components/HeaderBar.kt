package com.piuu.launcher.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.SystemMetrics
import com.piuu.launcher.ui.theme.*

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
    var showDevTools by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Main Search & Companion Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(LauncherGlass)
                .border(1.dp, LauncherBorder.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Friendly Search Pill (Daily User Concept)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Search apps or ask AI...",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Companion Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Piuu Brain AI Companion
                IconButton(
                    onClick = onOpenBrainChat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Piuu Brain AI",
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Marketplace / Themes Hub
                IconButton(
                    onClick = onOpenMarketplace,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Marketplace",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings / Control Center Quick Preferences
                IconButton(
                    onClick = onOpenQuickSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Control Center",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Toggle for Developer / Advanced Utilities
                IconButton(
                    onClick = { showDevTools = !showDevTools },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (showDevTools) Icons.Default.ExpandLess else Icons.Default.MoreVert,
                        contentDescription = "More Developer Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Collapsible Advanced Developer Tools Row (Animate beautifully)
        AnimatedVisibility(
            visible = showDevTools,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(1.dp, LauncherBorder.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Telemetry Metrics
                    TextButton(
                        onClick = onOpenMetrics,
                        colors = ButtonDefaults.textButtonColors(contentColor = SuccessGreen)
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("System Metrics", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // 2. Raw JSON Schema IDE
                    TextButton(
                        onClick = onOpenSchemaEditor,
                        colors = ButtonDefaults.textButtonColors(contentColor = WarningAmber)
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schema IDE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // 3. Hybrid Web Playground
                    TextButton(
                        onClick = onOpenWebView,
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Web Sandbox", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
