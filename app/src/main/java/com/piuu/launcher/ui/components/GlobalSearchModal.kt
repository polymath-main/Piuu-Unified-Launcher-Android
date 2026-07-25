package com.piuu.launcher.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.SearchResultItem
import com.piuu.launcher.model.SystemApp
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchModal(
    visible: Boolean,
    apps: List<SystemApp>,
    onDismiss: () -> Unit,
    onLaunchApp: (SystemApp) -> Unit
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }
    var recentQueries by remember { mutableStateOf(listOf("Marketplace", "Brain", "WiFi", "Metrics")) }

    val updateRecentSearches: () -> Unit = {
        if (query.isNotBlank()) {
            val trimmed = query.trim()
            val updated = (listOf(trimmed) + recentQueries.filter { it.lowercase() != trimmed.lowercase() }).take(4)
            recentQueries = updated
        }
    }

    val appResults = apps.filter { it.name.contains(query, ignoreCase = true) }.take(4)

    val mockSystemActions = listOf(
        SearchResultItem("s1", "Toggle Wi-Fi Connection", "System Setting", "setting", "toggle_wifi", "wifi"),
        SearchResultItem("s2", "Open Marketplace Catalog", "Piuu Launcher", "setting", "open_marketplace", "shopping_bag"),
        SearchResultItem("s3", "View Live Metrics Dashboard", "Piuu Launcher", "setting", "open_metrics", "bar_chart"),
        SearchResultItem("s4", "Edit Launcher Schema JSON", "Piuu IDE", "setting", "open_schema", "code")
    ).filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }

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
                Text("Universal Search", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps, settings, actions...", color = TextMuted) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = LauncherBorder,
                    focusedContainerColor = CardGlassBg,
                    unfocusedContainerColor = CardGlassBg,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (query.isEmpty()) {
                    // Dynamic top 4 used apps
                    val popularApps = apps.sortedByDescending { it.usage_count }.take(4)
                    if (popularApps.isNotEmpty()) {
                        item {
                            Text("Popular Shortcuts", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                popularApps.forEach { app ->
                                    AssistChip(
                                        onClick = { onLaunchApp(app); onDismiss() },
                                        label = { Text(app.name, fontSize = 11.sp, color = TextPrimary) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = CardGlassBg,
                                            labelColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Interactive recent searches
                    if (recentQueries.isNotEmpty()) {
                        item {
                            Text("Recent Searches", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentQueries.forEach { q ->
                                    SuggestionChip(
                                        onClick = { query = q },
                                        label = { Text(q, fontSize = 11.sp, color = TextPrimary) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = CardGlassBg,
                                            labelColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                if (appResults.isNotEmpty()) {
                    item {
                        Text("Matching Apps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                    items(appResults, key = { it.package_name }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardGlassBg)
                                .clickable {
                                    updateRecentSearches()
                                    onLaunchApp(app)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = PrimaryBlue)
                            }
                            Column {
                                Text(app.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(app.package_name, fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }

                if (mockSystemActions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("System Shortcuts & Actions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                    items(mockSystemActions, key = { it.id }) { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardGlassBg)
                                .clickable {
                                    updateRecentSearches()
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = AccentPurple)
                            }
                            Column {
                                Text(action.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(action.subtitle, fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
