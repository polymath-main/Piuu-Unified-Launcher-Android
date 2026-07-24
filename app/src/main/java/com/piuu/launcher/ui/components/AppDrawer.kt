package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.SystemApp
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    visible: Boolean,
    apps: List<SystemApp>,
    onDismiss: () -> Unit,
    onLaunchApp: (SystemApp) -> Unit,
    onToggleFavorite: (SystemApp) -> Unit
) {
    if (!visible) return

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }

    val categories = listOf("all", "piuu_suite", "social", "productivity", "entertainment", "utilities", "system")

    val filteredApps = apps.filter { app ->
        val matchesQuery = app.name.contains(searchQuery, ignoreCase = true) || app.package_name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategory == "all") true else app.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCategory
    }

    val frequentApps = apps.sortedByDescending { it.usage_count }.take(5)

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
                // Header Search Input & Close Button Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search ${apps.size} installed apps...", color = TextMuted) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = LauncherBorder,
                            focusedContainerColor = CardGlassBg,
                            unfocusedContainerColor = CardGlassBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category.replace("_", " ").replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            containerColor = CardGlassBg
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frequently Used Row (if no search query)
            if (searchQuery.isEmpty() && selectedCategory == "all") {
                Text("Frequently Used", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(frequentApps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onLaunchApp(app) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getAppIconVector(app.icon_name),
                                    contentDescription = app.name,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = app.name, fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("All Apps (${filteredApps.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            // App Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps) { app ->
                    AppGridTileItem(
                        app = app,
                        onClick = { onLaunchApp(app) }
                    )
                }
            }
        }
    }
}
}

@Composable
fun AppGridTileItem(app: SystemApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        BadgedBox(
            badge = {
                if (app.badge_count > 0) {
                    Badge(containerColor = DangerRed) { Text("${app.badge_count}") }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardGlassBg)
                    .border(1.dp, LauncherBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAppIconVector(app.icon_name),
                    contentDescription = app.name,
                    tint = if (app.category == "piuu_suite") SuccessGreen else PrimaryBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun getAppIconVector(iconName: String) = when(iconName) {
    "phone" -> Icons.Default.Phone
    "message" -> Icons.Default.Message
    "globe" -> Icons.Default.Public
    "camera" -> Icons.Default.CameraAlt
    "brain" -> Icons.Default.AutoAwesome
    "folder" -> Icons.Default.Folder
    "code" -> Icons.Default.Code
    "music" -> Icons.Default.MusicNote
    "mail" -> Icons.Default.Mail
    "video" -> Icons.Default.PlayCircle
    "calendar" -> Icons.Default.CalendarToday
    "settings" -> Icons.Default.Settings
    "note" -> Icons.Default.EditNote
    "calculator" -> Icons.Default.Calculate
    "cloud" -> Icons.Default.Cloud
    "file" -> Icons.Default.InsertDriveFile
    "clock" -> Icons.Default.AccessTime
    else -> Icons.Default.Apps
}
