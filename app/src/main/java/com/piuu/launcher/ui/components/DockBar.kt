package com.piuu.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.piuu.launcher.ui.theme.*

@Composable
fun DockBar(
    dockApps: List<SystemApp>,
    onLaunchApp: (SystemApp) -> Unit,
    onOpenDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Swipe Up Handle Indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(LauncherGlass)
                .clickable { onOpenDrawer() }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Swipe up", tint = TextSecondary, modifier = Modifier.size(16.dp))
            Text("App Drawer", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Dock Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(LauncherGlass)
                .border(1.dp, LauncherBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.forEach { app ->
                DockIconItem(app = app, onClick = { onLaunchApp(app) })
            }
        }
    }
}

@Composable
fun DockIconItem(app: SystemApp, onClick: () -> Unit) {
    BadgedBox(
        badge = {
            if (app.badge_count > 0) {
                Badge(containerColor = DangerRed) {
                    Text("${app.badge_count}", color = Color.White)
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.25f))
                .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when(app.icon_name) {
                    "phone" -> Icons.Default.Phone
                    "message" -> Icons.Default.Message
                    "globe" -> Icons.Default.Public
                    "camera" -> Icons.Default.CameraAlt
                    "brain" -> Icons.Default.AutoAwesome
                    else -> Icons.Default.Apps
                },
                contentDescription = app.name,
                tint = if (app.icon_name == "brain") AccentPurple else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
