package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.piuu.launcher.model.MarketplaceItem
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceModal(
    visible: Boolean,
    catalog: List<MarketplaceItem>,
    onDismiss: () -> Unit,
    onInstallItem: (MarketplaceItem) -> Unit
) {
    if (!visible) return

    var selectedTab by remember { mutableStateOf("all") }
    val tabs = listOf("all", "theming", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills")

    val filteredItems = catalog.filter { item ->
        if (selectedTab == "all") true else item.category.equals(selectedTab, ignoreCase = true)
    }

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
                    Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Piuu Marketplace", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                containerColor = Color.Transparent,
                edgePadding = 0.dp
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.replaceFirstChar { it.uppercase() }, color = if (selectedTab == tab) PrimaryBlue else TextSecondary) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredItems) { item ->
                    MarketplaceCardItem(item = item, onInstall = { onInstallItem(item) })
                }
            }
        }
    }
}
}

@Composable
fun MarketplaceCardItem(item: MarketplaceItem, onInstall: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGlassBg),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when (item.category.lowercase()) {
                            "theming" -> PrimaryBlue
                            "fonts" -> AccentPurple
                            "layouts" -> SuccessGreen
                            "faces" -> WarningAmber
                            "widgets" -> WarningAmber
                            "icons" -> PrimaryBlue
                            "agents" -> AccentPurple
                            else -> SuccessGreen
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(item.category.lowercase()) {
                        "theming" -> Icons.Default.Palette
                        "fonts" -> Icons.Default.FormatSize
                        "layouts" -> Icons.Default.Dashboard
                        "faces" -> Icons.Default.AccessTime
                        "widgets" -> Icons.Default.Widgets
                        "icons" -> Icons.Default.Apps
                        "agents" -> Icons.Default.SmartToy
                        "skills" -> Icons.Default.Bolt
                        else -> Icons.Default.Extension
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (item.preview_badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = PrimaryBlue) { Text(item.preview_badge, color = Color.White, fontSize = 9.sp) }
                    }
                }
                Text("${item.author} • ⭐ ${item.rating} (${item.downloads} downloads)", fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
            }

            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.is_installed) CardGlassBg else PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (item.is_installed) "Applied" else "Get",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
