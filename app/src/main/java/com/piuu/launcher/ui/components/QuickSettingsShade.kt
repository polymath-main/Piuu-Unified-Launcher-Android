package com.piuu.launcher.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.NotificationItem
import com.piuu.launcher.model.SystemMetrics
import com.piuu.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsShade(
    visible: Boolean,
    metrics: SystemMetrics,
    notifications: List<NotificationItem>,
    showSystemWallpaper: Boolean,
    onToggleWallpaper: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onDismiss: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleBatterySaver: () -> Unit,
    onToggleDnd: () -> Unit,
    onClearNotifications: () -> Unit,
    onScanApps: () -> Unit
) {
    if (!visible) return

    val context = androidx.compose.ui.platform.LocalContext.current

    // Real Permission state trackers for Production-Grade gateway
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    "android.permission.POST_NOTIFICATIONS"
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val prefManager = remember { com.piuu.launcher.repository.LauncherPreferenceManager.getInstance(context) }
    var gestureSwipeUp by remember { mutableStateOf(prefManager.gestureSwipeUp) }
    var gestureSwipeDown by remember { mutableStateOf(prefManager.gestureSwipeDown) }
    var gestureDoubleTap by remember { mutableStateOf(prefManager.gestureDoubleTap) }
    var gestureSwipeLeft by remember { mutableStateOf(prefManager.gestureSwipeLeft) }
    var gestureSwipeRight by remember { mutableStateOf(prefManager.gestureSwipeRight) }

    // Permission launchers
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val storagePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Control Center & Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Scanning & Syncing Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Application Management", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Import Installed Apps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Scan device Package Manager for launcher applications on Android 16", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onScanApps,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan & Import System Apps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wallpaper Config Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Live Wallpaper Options", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show System Wallpaper", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Renders the Android system wallpaper behind homescreen surfaces", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = showSystemWallpaper,
                                onCheckedChange = { onToggleWallpaper() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryBlue,
                                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onChangeWallpaper,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change System Wallpaper Intent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Homescreen Gestures Configuration Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Homescreen Gesture Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Map touch gestures to native launcher actions", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GestureRowSetting(
                            title = "Swipe Up",
                            gestureIcon = Icons.Default.SwipeUp,
                            currentAction = gestureSwipeUp,
                            onActionSelected = { action ->
                                gestureSwipeUp = action
                                prefManager.gestureSwipeUp = action
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(6.dp))

                        GestureRowSetting(
                            title = "Swipe Down",
                            gestureIcon = Icons.Default.SwipeDown,
                            currentAction = gestureSwipeDown,
                            onActionSelected = { action ->
                                gestureSwipeDown = action
                                prefManager.gestureSwipeDown = action
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(6.dp))

                        GestureRowSetting(
                            title = "Double Tap",
                            gestureIcon = Icons.Default.TouchApp,
                            currentAction = gestureDoubleTap,
                            onActionSelected = { action ->
                                gestureDoubleTap = action
                                prefManager.gestureDoubleTap = action
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(6.dp))

                        GestureRowSetting(
                            title = "Swipe Left",
                            gestureIcon = Icons.Default.West,
                            currentAction = gestureSwipeLeft,
                            onActionSelected = { action ->
                                gestureSwipeLeft = action
                                prefManager.gestureSwipeLeft = action
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(6.dp))

                        GestureRowSetting(
                            title = "Swipe Right",
                            gestureIcon = Icons.Default.East,
                            currentAction = gestureSwipeRight,
                            onActionSelected = { action ->
                                gestureSwipeRight = action
                                prefManager.gestureSwipeRight = action
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Permissions Gateway Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Permissions Gateway", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Keep operations secure & crash-free", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionRowItem(
                            title = "Notifications Access",
                            description = "Needed to feed system alerts in Control Center.",
                            isGranted = hasNotificationPermission,
                            onRequest = {
                                if (android.os.Build.VERSION.SDK_INT >= 33) {
                                    notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(8.dp))

                        PermissionRowItem(
                            title = "Media Storage Access",
                            description = "Required to import custom icon packs & wallpapers.",
                            isGranted = hasStoragePermission,
                            onRequest = {
                                if (android.os.Build.VERSION.SDK_INT >= 33) {
                                    storagePermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                                } else {
                                    storagePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(8.dp))

                        PermissionRowItem(
                            title = "Camera Hardware Access",
                            description = "Required for flashlight controller toggle features.",
                            isGranted = hasCameraPermission,
                            onRequest = {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // System Metrics Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Live System Health", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricItem(title = "CPU", value = "${metrics.cpu_usage}%", color = PrimaryBlue)
                            MetricItem(title = "RAM", value = "${metrics.ram_used_mb} GB / ${metrics.ram_total_mb} GB", color = AccentPurple)
                            MetricItem(title = "Battery", value = "${metrics.battery_pct}%", color = SuccessGreen)
                            MetricItem(title = "Network", value = "${(metrics.network_speed_kbps / 1000).toInt()} MB/s", color = WarningAmber)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun QuickToggleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (active) activeColor.copy(alpha = 0.25f) else CardGlassBg
    val border = if (active) activeColor else LauncherBorder
    val iconTint = if (active) activeColor else TextMuted

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (active) activeColor else Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (active) Color.White else iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun MetricItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = title, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
fun NotificationCardItem(item: NotificationItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(item.icon_name) {
                        "brain" -> Icons.Default.AutoAwesome
                        "message" -> Icons.Default.Message
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = item.app_name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(text = item.time, fontSize = 10.sp, color = TextMuted)
                }
                Text(text = item.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = item.message, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun TextTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryBlue,
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun PermissionRowItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = description, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessGreen.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Granted", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onRequest,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Grant", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun GestureRowSetting(
    title: String,
    gestureIcon: ImageVector,
    currentAction: String,
    onActionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = com.piuu.launcher.repository.LauncherPreferenceManager.GESTURE_ACTIONS.find { it.first == currentAction }?.second ?: currentAction

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = gestureIcon,
                    contentDescription = title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    text = currentLabel,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
            ) {
                Text(
                    text = currentLabel,
                    fontSize = 11.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                com.piuu.launcher.repository.LauncherPreferenceManager.GESTURE_ACTIONS.forEach { (actionKey, actionLabel) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                actionLabel,
                                fontSize = 12.sp,
                                color = if (actionKey == currentAction) PrimaryBlue else TextPrimary,
                                fontWeight = if (actionKey == currentAction) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onActionSelected(actionKey)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
