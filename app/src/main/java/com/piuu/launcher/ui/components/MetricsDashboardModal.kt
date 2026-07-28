package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.SystemApp
import com.piuu.launcher.model.SystemMetrics
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsDashboardModal(
    visible: Boolean,
    metrics: SystemMetrics,
    apps: List<SystemApp>,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val topAppsByUsage = apps.sortedByDescending { it.usage_count }.take(6)
    val totalLaunches = apps.sumOf { it.usage_count }
    val maxUsage = apps.maxOfOrNull { it.usage_count }?.coerceAtLeast(1) ?: 1

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LauncherBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launcher Metrics & Usage", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Screen Time & Activity Summary Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Today Screen Time", fontSize = 13.sp, color = TextSecondary)
                            Text("4 hours 12 mins", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Launches: $totalLaunches times", fontSize = 12.sp, color = SuccessGreen)
                                Text("Active Apps: ${apps.filter { it.usage_count > 0 }.size}", fontSize = 12.sp, color = PrimaryBlue)
                            }
                        }
                    }
                }

                // App Usage Frequency
                item {
                    Text("App Usage Frequency", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                items(topAppsByUsage, key = { it.package_name }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardGlassBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = PrimaryBlue)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = (app.usage_count.toFloat() / maxUsage.toFloat()).coerceIn(0.1f, 1f),
                                color = PrimaryBlue,
                                trackColor = Color(0x22FFFFFF),
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            )
                        }
                        Text("${app.usage_count} opens", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }

                // System Performance Status
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hardware Performance Radar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CPU Load", color = TextSecondary, fontSize = 13.sp)
                                Text("${metrics.cpu_usage}% Normal", color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RAM Memory", color = TextSecondary, fontSize = 13.sp)
                                Text("${metrics.ram_used_mb} GB / ${metrics.ram_total_mb} GB", color = AccentPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Internal Storage", color = TextSecondary, fontSize = 13.sp)
                                Text("${metrics.storage_used_gb} GB / ${metrics.storage_total_gb} GB", color = WarningAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
