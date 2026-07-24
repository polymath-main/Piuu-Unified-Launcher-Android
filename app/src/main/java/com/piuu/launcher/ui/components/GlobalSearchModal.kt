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

    val appResults = apps.filter { it.name.contains(query, ignoreCase = true) }.take(4)

    val mockSystemActions = listOf(
        SearchResultItem("s1", "Toggle Wi-Fi Connection", "System Setting", "setting", "toggle_wifi", "wifi"),
        SearchResultItem("s2", "Open Marketplace Catalog", "Piuu Launcher", "setting", "open_marketplace", "shopping_bag"),
        SearchResultItem("s3", "View Live Metrics Dashboard", "Piuu Launcher", "setting", "open_metrics", "bar_chart"),
        SearchResultItem("s4", "Edit Launcher Schema JSON", "Piuu IDE", "setting", "open_schema", "code")
    ).filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LauncherBackground,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                if (appResults.isNotEmpty()) {
                    item {
                        Text("Matching Apps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                    items(appResults) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardGlassBg)
                                .clickable { onLaunchApp(app); onDismiss() }
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
                    items(mockSystemActions) { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardGlassBg)
                                .clickable { onDismiss() }
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
