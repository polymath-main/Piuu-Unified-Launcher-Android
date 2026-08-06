package com.piuu.launcher.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.*
import com.piuu.launcher.repository.AiEngine
import com.piuu.launcher.repository.LatencyManager
import com.piuu.launcher.repository.LibC
import com.piuu.launcher.repository.LauncherConfigManager
import com.piuu.launcher.repository.LauncherPreferenceManager
import com.piuu.launcher.repository.LauncherRepository
import com.piuu.launcher.repository.NotificationBridge
import com.piuu.launcher.ui.theme.*

/**
 * NativeControlDashboard: A fully native Jetpack Compose replacement for the legacy HTML hybrid sandbox.
 * Implements a high-performance, beautiful control hub for launcher performance, tuning, and shortcuts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeControlDashboard(
    repository: LauncherRepository,
    latencyManager: LatencyManager,
    aiEngine: AiEngine,
    schema: LauncherSchema,
    onSaveSchema: (LauncherSchema) -> Unit,
    onDismiss: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenSchemaEditor: () -> Unit,
    onPrefChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configManager = remember { LauncherConfigManager.getInstance(context) }
    val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
    var config by remember { mutableStateOf(configManager.config) }

    val notificationBridge = remember { NotificationBridge.getInstance(context) }
    val notifications by notificationBridge.notificationsFlow.collectAsState()

    var activeTab by remember { mutableStateOf("tuning") } // "tuning", "notifications"

    val apps = remember { repository.getApps() }
    val avgLatency = remember { latencyManager.getAverageLaunchLatencyMs() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LauncherBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DashboardCustomize,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Launcher Control Hub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Native Jetpack Engine",
                        fontSize = 11.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardGlassBg)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- TAB BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardGlassBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("tuning" to "System Tuning", "notifications" to "Notifications (${notifications.size})")
            tabs.forEach { (tabId, label) ->
                val isActive = activeTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) PrimaryBlue else Color.Transparent)
                        .clickable { activeTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isActive) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- TAB CONTENT ---
        Box(modifier = Modifier.weight(1f)) {
            if (activeTab == "tuning") {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Quick Status Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, LauncherBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("APP LAUNCH LATENCY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${avgLatency}ms", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Ultra Low Latency", fontSize = 10.sp, color = SuccessGreen)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, LauncherBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("CLOUD SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Synchronized", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Secure Sandbox", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    // LibC Core Statistics Card
                    item {
                        val libCMemInfo = remember { LibC.getMemInfo() }
                        val libCAllocated by LibC.totalMemoryAllocated.collectAsState()
                        val libCThreads = remember { LibC.getThreadCount() }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LibC Unified Binary Core Status",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("NATIVE HEAP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(String.format("%.1f MB", libCMemInfo.nativeHeapAllocatedMb), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("UNIFIED ALLOC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(String.format("%.1f KB", libCAllocated / 1024.0), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("ACTIVE THREADS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$libCThreads active", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    }
                                }
                            }
                        }
                    }

                    // App Drawer Customization Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("App Drawer Customization", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Grid columns chooser
                                Text("Drawer Grid Columns", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val columns = listOf(3, 4, 5, 6)
                                    columns.forEach { col ->
                                        val isSelected = prefManager.drawerColumnCount == col
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else CardGlassBg)
                                                .border(1.dp, if (isSelected) PrimaryBlue else LauncherBorder, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    prefManager.drawerColumnCount = col
                                                    configManager.config = config.copy(drawerColumns = col)
                                                    config = configManager.config
                                                    onPrefChanged()
                                                    Toast.makeText(context, "Drawer set to $col columns", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$col Cols",
                                                color = if (isSelected) PrimaryBlue else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Scroll Orientation Chooser
                                var scrollMode by remember { mutableStateOf(prefManager.drawerScrollMode) }
                                Text("Scroll Direction", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val modes = listOf("VERTICAL" to "Vertical", "HORIZONTAL" to "Horizontal")
                                    modes.forEach { (modeVal, modeLabel) ->
                                        val isSelected = scrollMode == modeVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else CardGlassBg)
                                                .border(1.dp, if (isSelected) PrimaryBlue else LauncherBorder, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    prefManager.drawerScrollMode = modeVal
                                                    scrollMode = modeVal
                                                    onPrefChanged()
                                                    Toast.makeText(context, "Scroll set to $modeLabel style", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = modeLabel,
                                                color = if (isSelected) PrimaryBlue else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Show App Icon Labels Toggle
                                var showLabels by remember { mutableStateOf(prefManager.drawerShowLabels) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Show App Titles", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Display labels below app drawer icons", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = showLabels,
                                        onCheckedChange = { checked ->
                                            prefManager.drawerShowLabels = checked
                                            showLabels = checked
                                            onPrefChanged()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PrimaryBlue,
                                            checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Show Categories Tabs Toggle
                                var showCategories by remember { mutableStateOf(prefManager.drawerShowCategories) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Drawer Category Tabs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Group apps dynamically into smart categories", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = showCategories,
                                        onCheckedChange = { checked ->
                                            prefManager.drawerShowCategories = checked
                                            showCategories = checked
                                            onPrefChanged()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PrimaryBlue,
                                            checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Show Frequently Used Apps Toggle
                                var showFreq by remember { mutableStateOf(prefManager.drawerShowFrequentlyUsed) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Frequently Used Apps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Display a row of recent launches at top", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = showFreq,
                                        onCheckedChange = { checked ->
                                            prefManager.drawerShowFrequentlyUsed = checked
                                            showFreq = checked
                                            onPrefChanged()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PrimaryBlue,
                                            checkedTrackColor = PrimaryBlue.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // App Drawer Blur slider
                                var blurValue by remember { mutableStateOf(prefManager.appDrawerBlur.toFloat()) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Drawer Background Blur", fontSize = 12.sp, color = TextSecondary)
                                    Text("${blurValue.toInt()}dp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                }
                                Slider(
                                    value = blurValue,
                                    onValueChange = { blurValue = it },
                                    onValueChangeFinished = {
                                        prefManager.appDrawerBlur = blurValue.toInt()
                                        onPrefChanged()
                                        Toast.makeText(context, "Blur intensity set to ${blurValue.toInt()}dp", Toast.LENGTH_SHORT).show()
                                    },
                                    valueRange = 0f..30f,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = PrimaryBlue,
                                        thumbColor = PrimaryBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // App Drawer transparency slider
                                var transValue by remember { mutableStateOf(prefManager.appDrawerTransparency) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Drawer Transparency", fontSize = 12.sp, color = TextSecondary)
                                    Text("${(transValue * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                }
                                Slider(
                                    value = transValue,
                                    onValueChange = { transValue = it },
                                    onValueChangeFinished = {
                                        prefManager.appDrawerTransparency = transValue
                                        onPrefChanged()
                                        Toast.makeText(context, "Transparency updated", Toast.LENGTH_SHORT).show()
                                    },
                                    valueRange = 0.1f..1.0f,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = PrimaryBlue,
                                        thumbColor = PrimaryBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Homescreen & Dock Settings Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Homescreen & Dock Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // General App Icon Size slider
                                var sliderValue by remember { mutableStateOf(config.drawerIconSize.toFloat()) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("General Icon Size", fontSize = 12.sp, color = TextSecondary)
                                    Text("${sliderValue.toInt()}px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                }
                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = {
                                        configManager.config = config.copy(drawerIconSize = sliderValue.toInt(), homeIconSize = sliderValue.toInt())
                                        config = configManager.config
                                        onPrefChanged()
                                        Toast.makeText(context, "General icon size updated", Toast.LENGTH_SHORT).show()
                                    },
                                    valueRange = 40f..72f,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = AccentPurple,
                                        thumbColor = AccentPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Adaptive Icon Shapes chooser
                                var activeShape by remember { mutableStateOf(prefManager.adaptiveIconShape) }
                                Text("Adaptive Icon Shapes", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val shapesList = listOf(
                                        "ROUNDED_RECT" to "Square",
                                        "CIRCLE" to "Circle",
                                        "SQUIRCLE" to "Squircle",
                                        "TEARDROP" to "Teardrop"
                                    )
                                    shapesList.forEach { (shapeVal, shapeLabel) ->
                                        val isSelected = activeShape == shapeVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) AccentPurple.copy(alpha = 0.2f) else CardGlassBg)
                                                .border(1.dp, if (isSelected) AccentPurple else LauncherBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    prefManager.adaptiveIconShape = shapeVal
                                                    activeShape = shapeVal
                                                    onPrefChanged()
                                                    Toast.makeText(context, "Icon shape: $shapeLabel", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shapeLabel,
                                                color = if (isSelected) AccentPurple else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Dock Visibility Toggle
                                var dockVisible by remember { mutableStateOf(prefManager.dockVisible) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Enable Hotseat Dock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Show persistent bottom launch dock", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = dockVisible,
                                        onCheckedChange = { checked ->
                                            prefManager.dockVisible = checked
                                            dockVisible = checked
                                            onPrefChanged()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AccentPurple,
                                            checkedTrackColor = AccentPurple.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dock Columns Selector
                                Text("Dock Icons Limit", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val counts = listOf(3, 4, 5, 6)
                                    counts.forEach { count ->
                                        val isSelected = prefManager.dockIconCount == count
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) AccentPurple.copy(alpha = 0.2f) else CardGlassBg)
                                                .border(1.dp, if (isSelected) AccentPurple else LauncherBorder, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    prefManager.dockIconCount = count
                                                    onPrefChanged()
                                                    Toast.makeText(context, "Dock configured for max $count icons", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$count Apps",
                                                color = if (isSelected) AccentPurple else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Dock Icon Size slider
                                var dockSizeVal by remember { mutableStateOf(prefManager.dockIconSize.toFloat()) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dock Icons Size", fontSize = 12.sp, color = TextSecondary)
                                    Text("${dockSizeVal.toInt()}px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                }
                                Slider(
                                    value = dockSizeVal,
                                    onValueChange = { dockSizeVal = it },
                                    onValueChangeFinished = {
                                        prefManager.dockIconSize = dockSizeVal.toInt()
                                        onPrefChanged()
                                        Toast.makeText(context, "Dock icon size updated", Toast.LENGTH_SHORT).show()
                                    },
                                    valueRange = 40f..72f,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = AccentPurple,
                                        thumbColor = AccentPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Dock Labels Visibility
                                var dockLabelsVisible by remember { mutableStateOf(prefManager.dockShowLabels) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Show Dock App Labels", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Show text titles below dock icons", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = dockLabelsVisible,
                                        onCheckedChange = { checked ->
                                            prefManager.dockShowLabels = checked
                                            dockLabelsVisible = checked
                                            onPrefChanged()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AccentPurple,
                                            checkedTrackColor = AccentPurple.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Show System Wallpaper Toggle
                                var systemWallpaperEnabled by remember { mutableStateOf(config.showSystemWallpaper) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Show System Wallpaper", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Allow active system wallpaper background", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = systemWallpaperEnabled,
                                        onCheckedChange = { checked ->
                                            configManager.setWallpaperEnabled(checked)
                                            config = configManager.config
                                            systemWallpaperEnabled = checked
                                            onPrefChanged()
                                            Toast.makeText(context, if (checked) "Wallpaper transparency unlocked" else "Dynamic AMOLED black background applied", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AccentPurple,
                                            checkedTrackColor = AccentPurple.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Shortcuts to sub-systems
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, LauncherBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Launcher Hub Integration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenMarketplace()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Marketplace", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenSchemaEditor()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Schema IDE", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Notifications tab content
                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active notifications",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent System Notifications",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Clear All",
                                color = PrimaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        notificationBridge.clearAllNotifications()
                                        Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(notifications, key = { it.id }) { notif ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, LauncherBorder, RoundedCornerShape(14.dp)),
                                    colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                         Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = notif.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = notif.message,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                notificationBridge.dismissNotification(notif.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Dismiss",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
