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
import com.piuu.launcher.repository.LauncherPreferenceManager
import com.piuu.launcher.repository.FloatingOverlayService
import com.piuu.launcher.repository.HardwareControlBridge
import com.piuu.launcher.isServiceRunning
import com.piuu.launcher.toggleFloatingOverlay

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
    onScanApps: () -> Unit,
    isDarkMode: Boolean = true,
    onToggleDarkMode: ((Boolean) -> Unit)? = null,
    onAutoCategorize: (() -> Unit) -> Unit = {}
) {
    if (!visible) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val hardwareBridge = remember { HardwareControlBridge.getInstance(context) }
    val isFlashlightActive by hardwareBridge.isFlashlightOn.collectAsState()

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

    val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
    var gestureSwipeUp by remember { mutableStateOf(prefManager.gestureSwipeUp) }
    var gestureSwipeDown by remember { mutableStateOf(prefManager.gestureSwipeDown) }
    var gestureDoubleTap by remember { mutableStateOf(prefManager.gestureDoubleTap) }
    var gestureSwipeLeft by remember { mutableStateOf(prefManager.gestureSwipeLeft) }
    var gestureSwipeRight by remember { mutableStateOf(prefManager.gestureSwipeRight) }
    var gestureEdgeSwipe by remember { mutableStateOf(prefManager.gestureEdgeSwipe) }
    var drawerColumnCount by remember { mutableStateOf(prefManager.drawerColumnCount) }
    var drawerShowFrequentlyUsed by remember { mutableStateOf(prefManager.drawerShowFrequentlyUsed) }
    var drawerShowCategories by remember { mutableStateOf(prefManager.drawerShowCategories) }

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

                Spacer(modifier = Modifier.height(14.dp))

                // ── Quick Hardware & System Tiles Grid ────────────────────────────
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Quick Controls",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 1: Wi-Fi, Bluetooth, Flashlight, DND
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "Wi-Fi",
                                icon = Icons.Default.Wifi,
                                isActive = true,
                                activeColor = PrimaryBlue,
                                onClick = { hardwareBridge.openWifiPanel(context) }
                            )
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "Bluetooth",
                                icon = Icons.Default.Bluetooth,
                                isActive = metrics.bluetooth_enabled,
                                activeColor = PrimaryBlue,
                                onClick = { hardwareBridge.openBluetoothPanel(context) }
                            )
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "Torch",
                                icon = Icons.Default.FlashlightOn,
                                isActive = isFlashlightActive,
                                activeColor = WarningAmber,
                                onClick = {
                                    if (hasCameraPermission) {
                                        hardwareBridge.toggleFlashlight()
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                            )
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "DND",
                                icon = Icons.Default.DoNotDisturb,
                                isActive = metrics.do_not_disturb,
                                activeColor = DangerRed,
                                onClick = { hardwareBridge.toggleDndMode(context) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Battery Saver, Default Home, Wallpaper, Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "Battery",
                                icon = Icons.Default.BatteryChargingFull,
                                isActive = metrics.battery_pct < 20 || metrics.battery_saver,
                                activeColor = SuccessGreen,
                                onClick = { hardwareBridge.openBatterySaverSettings(context) }
                            )
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "Home App",
                                icon = Icons.Default.Home,
                                isActive = true,
                                activeColor = AccentPurple,
                                onClick = { hardwareBridge.openDefaultLauncherSelector(context) }
                            )
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "Wallpaper",
                                icon = Icons.Default.Wallpaper,
                                isActive = showSystemWallpaper,
                                activeColor = PrimaryBlue,
                                onClick = { hardwareBridge.openWallpaperPicker(context) }
                            )
                            QuickTileItem(
                                modifier = Modifier.weight(1f),
                                label = "Settings",
                                icon = Icons.Default.Settings,
                                isActive = false,
                                activeColor = TextPrimary,
                                onClick = { hardwareBridge.openSystemSettings(context) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Display Theme Mode Card
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
                                Text("Display Theme Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(if (isDarkMode) "Dark Theme active" else "Light Theme active", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Dark Mode Option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkMode) PrimaryBlue else CardGlassBg)
                                    .border(1.dp, if (isDarkMode) PrimaryBlue else LauncherBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        onToggleDarkMode?.invoke(true)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = if (isDarkMode) Color.White else TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Dark Mode",
                                        fontSize = 12.sp,
                                        fontWeight = if (isDarkMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isDarkMode) Color.White else TextPrimary
                                    )
                                }
                            }

                            // Light Mode Option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isDarkMode) PrimaryBlue else CardGlassBg)
                                    .border(1.dp, if (!isDarkMode) PrimaryBlue else LauncherBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        onToggleDarkMode?.invoke(false)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (!isDarkMode) Color.White else TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Light Mode",
                                        fontSize = 12.sp,
                                        fontWeight = if (!isDarkMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isDarkMode) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
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

                // Floating Overlay (Draw Over Other Apps) PIP Card
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Draw Over / Floating PIP Window", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Maintain PIP music/video & chat heads while using other apps", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val context = androidx.compose.ui.platform.LocalContext.current
                        var isOverlayActive by remember {
                            mutableStateOf(isServiceRunning(context, FloatingOverlayService::class.java))
                        }
                        val canDrawOver = remember { android.provider.Settings.canDrawOverlays(context) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Seamless PIP Overlay Engine", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    if (canDrawOver) "Permission Granted • Active floating window" else "Requires 'Draw over other apps' permission",
                                    fontSize = 11.sp,
                                    color = if (canDrawOver) SuccessGreen else WarningAmber
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isOverlayActive,
                                onCheckedChange = {
                                    toggleFloatingOverlay(context)
                                    isOverlayActive = isServiceRunning(context, FloatingOverlayService::class.java)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryBlue,
                                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                )
                            )
                        }

                        if (!canDrawOver) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    toggleFloatingOverlay(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Draw Over Permission", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
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

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(6.dp))

                        GestureRowSetting(
                            title = "Edge Swipe (Side/Top/Bottom Edge)",
                            gestureIcon = Icons.Default.Swipe,
                            currentAction = gestureEdgeSwipe,
                            onActionSelected = { action ->
                                gestureEdgeSwipe = action
                                prefManager.gestureEdgeSwipe = action
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Drawer Layout Configuration Card
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
                                Text("App Drawer Layout Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Customize grids, columns, and navigation chips", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(imageVector = Icons.Default.GridView, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Column count selector
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Grid Columns", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Number of items in each row of the app drawer grid", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(3, 4, 5, 6).forEach { cols ->
                                    val isSelected = drawerColumnCount == cols
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PrimaryBlue else CardGlassBg)
                                            .border(1.dp, if (isSelected) PrimaryBlue else LauncherBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                drawerColumnCount = cols
                                                prefManager.drawerColumnCount = cols
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$cols Cols",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Frequently Used Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Frequently Used Apps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Show a quick row of your most launched apps at the top", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = drawerShowFrequentlyUsed,
                                onCheckedChange = { checked ->
                                    drawerShowFrequentlyUsed = checked
                                    prefManager.drawerShowFrequentlyUsed = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryBlue,
                                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Chips Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Category Navigation Chips", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Filter applications by categories (Utilities, Social, productivity...)", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = drawerShowCategories,
                                onCheckedChange = { checked ->
                                    drawerShowCategories = checked
                                    prefManager.drawerShowCategories = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryBlue,
                                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scroll mode selector
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Drawer Scroll Direction", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Page navigation layout pattern of the app drawer list", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var scrollModeState by remember { mutableStateOf(prefManager.drawerScrollMode) }
                                listOf("VERTICAL" to "Vertical Grid", "HORIZONTAL" to "Horizontal Paged").forEach { (mode, label) ->
                                    val isSelected = scrollModeState == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PrimaryBlue else CardGlassBg)
                                            .border(1.dp, if (isSelected) PrimaryBlue else LauncherBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                scrollModeState = mode
                                                prefManager.drawerScrollMode = mode
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Drawer Background Card Customization
                        Column(modifier = Modifier.fillMaxWidth()) {
                            var drawerTrans by remember { mutableStateOf(prefManager.appDrawerTransparency) }
                            Text("Background Card Transparency: ${(drawerTrans * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Adjust opacity of the drawer backdrop card", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = drawerTrans,
                                onValueChange = {
                                    drawerTrans = it
                                    prefManager.appDrawerTransparency = it
                                },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(activeTrackColor = PrimaryBlue, thumbColor = PrimaryBlue)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Drawer Backdrop Theme Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            var drawerBgHex by remember { mutableStateOf(prefManager.drawerBackgroundColor) }
                            val colorPresets = listOf(
                                "#020817" to "Slate",
                                "#0A0A0A" to "Midnight",
                                "#1E1E38" to "Indigo",
                                "#2E1B4E" to "Cyberpunk",
                                "#321010" to "Crimson"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorPresets.forEach { (hex, name) ->
                                    val parsed = parseRgbaOrHexColor(hex, PrimaryBlue)
                                    val isSel = drawerBgHex == hex
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(parsed)
                                            .border(
                                                width = if (isSel) 2.dp else 1.dp,
                                                color = if (isSel) PrimaryBlue else LauncherBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                drawerBgHex = hex
                                                prefManager.drawerBackgroundColor = hex
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Categorization Engine", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Automatically sort apps into categories like 'Social', 'Productivity', etc., using Gemini AI", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            var isCategorizing by remember { mutableStateOf(false) }
                            val context = androidx.compose.ui.platform.LocalContext.current
                            
                            Button(
                                onClick = {
                                    if (!isCategorizing) {
                                        isCategorizing = true
                                        onAutoCategorize {
                                            isCategorizing = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCategorizing) CardGlassBg else PrimaryBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (isCategorizing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Running...", fontSize = 11.sp, color = TextPrimary)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run AI", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dock Customization Card
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
                                Text("Dock Customization Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Personalize dock container backdrop, sizes, and labels", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(imageVector = Icons.Default.Dashboard, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dock Visibility Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Dock Bar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Toggle visibility of the home screen hotseat dock", fontSize = 11.sp, color = TextSecondary)
                            }
                            var dockVisibleState by remember { mutableStateOf(prefManager.dockVisible) }
                            Switch(
                                checked = dockVisibleState,
                                onCheckedChange = { checked ->
                                    dockVisibleState = checked
                                    prefManager.dockVisible = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryBlue,
                                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Dock App Labels Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show App Labels", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Render small text labels under icons in the dock", fontSize = 11.sp, color = TextSecondary)
                            }
                            var dockLabelsState by remember { mutableStateOf(prefManager.dockShowLabels) }
                            Switch(
                                checked = dockLabelsState,
                                onCheckedChange = { checked ->
                                    dockLabelsState = checked
                                    prefManager.dockShowLabels = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryBlue,
                                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Dock Max Icons Count
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Dock Max Icons count", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Select maximum number of apps to fit inside the dock", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            var dockIconCountState by remember { mutableStateOf(prefManager.dockIconCount) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(3, 4, 5, 6).forEach { count ->
                                    val isSelected = dockIconCountState == count
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PrimaryBlue else CardGlassBg)
                                            .border(1.dp, if (isSelected) PrimaryBlue else LauncherBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                dockIconCountState = count
                                                prefManager.dockIconCount = count
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$count Apps",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherBorder.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Dock Backdrop Style
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Backdrop visual theme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Adjust background material stylings for the dock bar", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            var dockBgStyleState by remember { mutableStateOf(prefManager.dockBackgroundStyle) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("GLASS" to "Frosted", "SOLID" to "Solid Dark", "TRANSPARENT" to "Clear").forEach { (style, label) ->
                                    val isSelected = dockBgStyleState == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PrimaryBlue else CardGlassBg)
                                            .border(1.dp, if (isSelected) PrimaryBlue else LauncherBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                dockBgStyleState = style
                                                prefManager.dockBackgroundStyle = style
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
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
    val currentLabel = LauncherPreferenceManager.GESTURE_ACTIONS.find { it.first == currentAction }?.second ?: currentAction

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
                LauncherPreferenceManager.GESTURE_ACTIONS.forEach { (actionKey, actionLabel) ->
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

@Composable
fun QuickTileItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.22f) else CardGlassBg
    val borderCol = if (isActive) activeColor.copy(alpha = 0.6f) else LauncherBorder
    val iconCol = if (isActive) activeColor else TextSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconCol,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) TextPrimary else TextSecondary,
            maxLines = 1
        )
    }
}

