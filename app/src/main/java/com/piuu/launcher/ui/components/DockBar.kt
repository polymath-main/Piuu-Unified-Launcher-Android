package com.piuu.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import com.piuu.launcher.model.SystemApp
import com.piuu.launcher.ui.theme.*
import com.piuu.launcher.repository.LauncherPreferenceManager
import com.piuu.launcher.repository.AppIconImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockBar(
    dockApps: List<SystemApp>,
    onLaunchApp: (SystemApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onLongPressApp: (SystemApp) -> Unit,
    dockBackgroundStyle: String = "GLASS",
    dockShowLabels: Boolean = false,
    dockVisible: Boolean = true
) {
    if (!dockVisible || dockBackgroundStyle == "HIDDEN") return

    val containerBackground = when (dockBackgroundStyle) {
        "SOLID" -> Color(0xFF0F172A) // Solid dark navy
        "TRANSPARENT" -> Color.Transparent
        else -> LauncherGlass // Glass mode
    }

    val containerBorder = if (dockBackgroundStyle == "TRANSPARENT") {
        Modifier
    } else {
        Modifier.border(1.dp, LauncherBorder, RoundedCornerShape(28.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
        val dockIconSize = prefManager.dockIconSize

        // Swipe Up Handle Indicator
        if (prefManager.drawerHandleVisible) {
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
        }

        // Main Dock Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(containerBackground)
                .then(containerBorder)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.forEach { app ->
                DockIconItem(
                    app = app,
                    showLabels = dockShowLabels,
                    iconSize = dockIconSize,
                    onClick = { onLaunchApp(app) },
                    onLongClick = { onLongPressApp(app) }
                )
            }
        }
    }
}

@Composable
fun DockIconItem(
    app: SystemApp,
    showLabels: Boolean,
    iconSize: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                    .size(iconSize.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.25f))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), CircleShape)
                    .longPressPulseEffect(
                        onClick = { onClick() },
                        onLongClick = { onLongClick() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppIconImage(
                    packageName = app.package_name,
                    modifier = Modifier.size((iconSize * 0.46f).dp),
                    fallbackIconName = app.icon_name,
                    tintColor = if (app.icon_name == "brain") AccentPurple else Color.White
                )
            }
        }
        if (showLabels) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.width(iconSize.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
