package com.piuu.launcher.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.*
import com.piuu.launcher.repository.LauncherPreferenceManager
import com.piuu.launcher.repository.NotificationBridge
import com.piuu.launcher.repository.WallpaperHandler
import com.piuu.launcher.repository.IconPackManager
import com.piuu.launcher.repository.LauncherRepository
import com.piuu.launcher.ui.theme.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.automirrored.filled.ArrowForward


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    schema: LauncherSchema,
    metrics: SystemMetrics,
    installedApps: List<SystemApp>,
    notes: List<NoteItem>,
    onLaunchApp: (SystemApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenBrainChat: () -> Unit,
    onOpenQuickSettings: () -> Unit = {},
    onOpenWebView: () -> Unit = {},
    onOpenMarketplace: () -> Unit = {},
    onOpenMetrics: () -> Unit = {},
    onOpenSchemaEditor: () -> Unit = {},
    onOpenFolder: () -> Unit,
    onAddNote: (String) -> Unit,
    onLongPressApp: (SystemApp) -> Unit,
    onLongPressElement: (LauncherElement) -> Unit = {},
    onSaveSchema: (LauncherSchema) -> Unit = {}
) {
    val context = LocalContext.current
    val pages = schema.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
    val notificationBridge = remember { NotificationBridge.getInstance(context) }
    val notifications by notificationBridge.notificationsFlow.collectAsState()

    var showInlineNotificationWidget by remember { mutableStateOf(false) }

    var activeResizingElementId by remember { mutableStateOf<String?>(null) }
    var showActionChooserForElement by remember { mutableStateOf<LauncherElement?>(null) }
    var showHomescreenOptions by remember { mutableStateOf(false) }
    var showWidgetsDashboard by remember { mutableStateOf(false) }

    val elementBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var draggedElement by remember { mutableStateOf<LauncherElement?>(null) }
    var draggedOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var currentTouchPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var hoveredElementId by remember { mutableStateOf<String?>(null) }
    var activeFolderToShow by remember { mutableStateOf<LauncherElement?>(null) }

    val currentPrefManager by rememberUpdatedState(prefManager)

    val executeAction: (String) -> Unit = { actionKey ->
        when (actionKey) {
            "DRAWER" -> onOpenDrawer()
            "SEARCH" -> onOpenSearch()
            "BRAIN_CHAT" -> onOpenBrainChat()
            "QUICK_SETTINGS" -> onOpenQuickSettings()
            "NOTIFICATION_CENTER" -> {
                showInlineNotificationWidget = !showInlineNotificationWidget
                onOpenQuickSettings()
            }
            "WEB_VIEW" -> onOpenWebView()
            "MARKETPLACE" -> onOpenMarketplace()
            "METRICS" -> onOpenMetrics()
            "SCHEMA_EDITOR" -> onOpenSchemaEditor()
            else -> {}
        }
    }

    val currentExecuteAction by rememberUpdatedState(executeAction)

    val dockApps = remember(schema.dock.app_packages, installedApps, prefManager.dockIconCount) {
        val mapped = schema.dock.app_packages.mapNotNull { pkg ->
            installedApps.find { it.package_name == pkg }
        }.ifEmpty { installedApps.take(prefManager.dockIconCount) }
        mapped.take(prefManager.dockIconCount)
    }

    var parentBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { parentBounds = it.boundsInWindow() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            currentExecuteAction(currentPrefManager.gestureDoubleTap)
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    val edgeThreshold = 48.dp.toPx()
                    val screenWidthPx = size.width.toFloat()
                    val screenHeightPx = size.height.toFloat()

                    val isLeftEdge = startX <= edgeThreshold
                    val isRightEdge = startX >= screenWidthPx - edgeThreshold
                    val isTopEdge = startY <= edgeThreshold
                    val isBottomEdge = startY >= screenHeightPx - edgeThreshold
                    val isEdgeStart = isLeftEdge || isRightEdge || isTopEdge || isBottomEdge

                    var totalX = 0f
                    var totalY = 0f
                    var gestureTriggered = false

                    val dragPointerId = down.id

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == dragPointerId } ?: continue
                        if (change.pressed) {
                            val dragAmount = change.positionChange()
                            totalX += dragAmount.x
                            totalY += dragAmount.y

                            val absX = kotlin.math.abs(totalX)
                            val absY = kotlin.math.abs(totalY)

                            if (!gestureTriggered && (absX > 30f || absY > 30f)) {
                                gestureTriggered = true
                                if (isEdgeStart) {
                                    // High-speed Edge Swipe Detection
                                    val action = currentPrefManager.gestureEdgeSwipe.ifEmpty { "NOTIFICATION_CENTER" }
                                    currentExecuteAction(action)
                                    change.consume()
                                } else {
                                    // High-speed Main Gesture Detection
                                    if (absY > absX) {
                                        if (totalY < -30f) {
                                            currentExecuteAction(currentPrefManager.gestureSwipeUp)
                                        } else if (totalY > 30f) {
                                            currentExecuteAction(currentPrefManager.gestureSwipeDown)
                                        }
                                    } else {
                                        if (totalX < -30f) {
                                            currentExecuteAction(currentPrefManager.gestureSwipeLeft)
                                        } else if (totalX > 30f) {
                                            currentExecuteAction(currentPrefManager.gestureSwipeRight)
                                        }
                                    }
                                    change.consume()
                                }
                            }
                        } else {
                            break
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        // Toggleable Notification Center Inline Banner / Widget
        AnimatedVisibility(
            visible = showInlineNotificationWidget,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            NotificationCenterWidgetComposable(
                notifications = notifications,
                onDismissNotification = { notificationBridge.dismissNotification(it) },
                onClearAll = { notificationBridge.clearAllNotifications() }
            )
        }

        // Edit Mode Status Banner
        AnimatedVisibility(
            visible = activeResizingElementId != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("📐 Edit Mode Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Use widget handles below to resize directly", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = { activeResizingElementId = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Homescreen Long-Press Unified Context Hub Options Dialog
        if (showHomescreenOptions) {
            val wallpaperHandler = remember { WallpaperHandler(context) }
            val iconPackManager = remember { IconPackManager(context) }
            val repository = remember { LauncherRepository(context) }

            Dialog(onDismissRequest = { showHomescreenOptions = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CardGlassBg.copy(alpha = 0.98f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, LauncherBorder),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Piuu Context Hub",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            IconButton(onClick = { showHomescreenOptions = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }

                        Text(
                            text = "Access system widgets, AI assistant, wallpaper colors, icon packs, and home grid controls.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. System Widget Picker
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                showWidgetsDashboard = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Widgets, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("System Widget Picker", fontWeight = FontWeight.Bold)
                        }

                        // 2. Gemini AI Assistant Launch
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                onOpenBrainChat()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Launch Piuu Brain AI", fontWeight = FontWeight.Bold)
                        }

                        // 3. App Drawer Navigation
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                onOpenDrawer()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardGlassBg),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Open App Drawer", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        // 4. Wallpaper Dynamic Palette Extraction
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                val palette = wallpaperHandler.extractWallpaperPalette()
                                val updatedTheme = schema.theme.copy(
                                    primary_color = palette.primaryHex,
                                    accent_color = palette.vibrantHex,
                                    bg_overlay = palette.bgOverlayHex
                                )
                                onSaveSchema(schema.copy(theme = updatedTheme))
                                Toast.makeText(context, "Harmonized dynamic colors from wallpaper palette!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardGlassBg),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Extract Wallpaper Dynamic Theme", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        // 5. Custom Icon Pack Scanner
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                val packs = iconPackManager.getInstalledIconPacks()
                                if (packs.isNotEmpty()) {
                                    Toast.makeText(context, "Detected ${packs.size} custom icon pack(s): ${packs.first().name}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Scanning completed: System dynamic adaptors active.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardGlassBg),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Brush, contentDescription = null, tint = AccentPurple)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Scan Custom Icon Packs", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        // 6. Home Grid Layout Reset
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                val defaultSchema = repository.resetSchema()
                                onSaveSchema(defaultSchema)
                                Toast.makeText(context, "Home grid layout reset to default!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardGlassBg),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Reset Home Grid Layout", color = DangerRed, fontWeight = FontWeight.SemiBold)
                        }

                        // 7. Launcher Preferences
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                onOpenQuickSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardGlassBg),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Launcher Preferences", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Widgets Dashboard Dialog
        if (showWidgetsDashboard) {
            Dialog(onDismissRequest = { showWidgetsDashboard = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CardGlassBg.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                ) {
                    var selectedTab by remember { mutableStateOf(0) } // 0 = Built-in, 1 = Default/System

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "🧩 Widgets Dashboard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Choose a widget to add to your workspace grid.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Tab Selection Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x11FFFFFF), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == 0) PrimaryBlue else Color.Transparent)
                                    .clickable { selectedTab = 0 }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Built-in App Widgets", color = if (selectedTab == 0) Color.White else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == 1) PrimaryBlue else Color.Transparent)
                                    .clickable { selectedTab = 1 }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("System / Third-Party", color = if (selectedTab == 1) Color.White else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable List of widgets
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedTab == 0) {
                                // Built-in App widgets
                                val builtinWidgets = listOf(
                                    Triple(ElementType.CLOCK, "Clock Widget", "Displays local current time & customizable clockface styles."),
                                    Triple(ElementType.WEATHER, "Weather Forecast", "Real-time updates, conditions, and microclimatic forecasts."),
                                    Triple(ElementType.BATTERY_STATUS, "Battery Widget", "Real-time battery percentage tracking and charge status."),
                                    Triple(ElementType.SYSTEM_STATS, "Performance Specs", "Monitors system resources like CPU usage, RAM and active storage."),
                                    Triple(ElementType.MEDIA_PLAYER, "Media Player", "Direct playback, track control, & sound volume levels."),
                                    Triple(ElementType.AGENT_WIDGET, "AI Brain Widget", "Neural-net assistant insights and quick generative agent cues."),
                                    Triple(ElementType.QUICK_NOTES, "Sticky Notes", "Instantly write down ideas or bulleted micro-reminders."),
                                    Triple(ElementType.PIUU_SUITE_FOLDER, "Suite Folder", "Preloaded folder compiling native suite and marketplace tools."),
                                    Triple(ElementType.PREDICTIVE_SUGGESTIONS, "App Suggestions", "Predictive application recommendations based on routines."),
                                    Triple(ElementType.NOTIFICATION_CENTER, "Notifications Panel", "Unified feed showing native notifications directly.")
                                )

                                items(builtinWidgets.size) { index ->
                                    val (type, name, desc) = builtinWidgets[index]
                                    WidgetDashboardItemRow(
                                        name = name,
                                        desc = desc,
                                        icon = when (type) {
                                            ElementType.CLOCK -> Icons.Default.AccessTime
                                            ElementType.WEATHER -> Icons.Default.Cloud
                                            ElementType.BATTERY_STATUS -> Icons.Default.BatteryChargingFull
                                            ElementType.SYSTEM_STATS -> Icons.Default.Memory
                                            ElementType.MEDIA_PLAYER -> Icons.Default.PlayArrow
                                            ElementType.AGENT_WIDGET -> Icons.Default.AutoAwesome
                                            ElementType.QUICK_NOTES -> Icons.Default.EditNote
                                            ElementType.PIUU_SUITE_FOLDER -> Icons.Default.Folder
                                            ElementType.PREDICTIVE_SUGGESTIONS -> Icons.Default.Star
                                            ElementType.NOTIFICATION_CENTER -> Icons.Default.Notifications
                                            else -> Icons.Default.Apps
                                        },
                                        onAdd = {
                                            val currentPageIndex = pagerState.currentPage
                                            val targetPage = pages.getOrNull(currentPageIndex)
                                            if (targetPage != null) {
                                                val newElem = LauncherElement(
                                                    element_id = "widget_${type.name.lowercase()}_${java.util.UUID.randomUUID().toString().take(5)}",
                                                    type = type,
                                                    title = name,
                                                    style_props = StyleProps(
                                                        w = if (type == ElementType.CLOCK || type == ElementType.WEATHER || type == ElementType.MEDIA_PLAYER || type == ElementType.AGENT_WIDGET || type == ElementType.QUICK_NOTES || type == ElementType.NOTIFICATION_CENTER) 4 else 2,
                                                        h = 2,
                                                        borderRadius = 20,
                                                        accentColor = "#3B82F6"
                                                    )
                                                )
                                                val updatedElements = targetPage.elements + newElem
                                                val updatedPages = pages.mapIndexed { idx, page ->
                                                    if (idx == currentPageIndex) page.copy(elements = updatedElements) else page
                                                }
                                                onSaveSchema(schema.copy(pages = updatedPages))
                                                Toast.makeText(context, "Added '$name' to your homescreen!", Toast.LENGTH_SHORT).show()
                                                showWidgetsDashboard = false
                                            }
                                        }
                                    )
                                }
                            } else {
                                // Default / System simulated widgets
                                val defaultWidgets = listOf(
                                    Triple("spotify", "Spotify Player", "Integrates Spotify music stream controls on your homescreen directly."),
                                    Triple("google_search", "Google Search", "Full-width Google Search bar with quick speech dictation capabilities."),
                                    Triple("google_calendar", "Google Calendar Agenda", "Shows agenda list and meetings directly linked to system calendar.")
                                )

                                items(defaultWidgets.size) { index ->
                                    val (id, name, desc) = defaultWidgets[index]
                                    WidgetDashboardItemRow(
                                        name = name,
                                        desc = desc,
                                        icon = when (id) {
                                            "spotify" -> Icons.Default.MusicNote
                                            "google_search" -> Icons.Default.Search
                                            "google_calendar" -> Icons.Default.CalendarToday
                                            else -> Icons.Default.Apps
                                        },
                                        onAdd = {
                                            val currentPageIndex = pagerState.currentPage
                                            val targetPage = pages.getOrNull(currentPageIndex)
                                            if (targetPage != null) {
                                                val newElem = LauncherElement(
                                                    element_id = "syswidget_${id}_${java.util.UUID.randomUUID().toString().take(5)}",
                                                    type = ElementType.CUSTOM_CARD,
                                                    title = name,
                                                    style_props = StyleProps(
                                                        w = 4,
                                                        h = 2,
                                                        borderRadius = 20,
                                                        accentColor = if (id == "spotify") "#1DB954" else "#3B82F6"
                                                    ),
                                                    data_props = mapOf("system_widget_id" to id)
                                                )
                                                val updatedElements = targetPage.elements + newElem
                                                val updatedPages = pages.mapIndexed { idx, page ->
                                                    if (idx == currentPageIndex) page.copy(elements = updatedElements) else page
                                                }
                                                onSaveSchema(schema.copy(pages = updatedPages))
                                                Toast.makeText(context, "Added system widget '$name'!", Toast.LENGTH_SHORT).show()
                                                showWidgetsDashboard = false
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showWidgetsDashboard = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Text("Close", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Widget Action Chooser Dialog
        if (showActionChooserForElement != null) {
            val elem = showActionChooserForElement!!
            Dialog(onDismissRequest = { showActionChooserForElement = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = activeBgColor.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Widget Actions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose an option to customize or resize this widget.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                activeResizingElementId = elem.element_id
                                showActionChooserForElement = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📐 Resize Widget (Edit Mode)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onLongPressElement(elem)
                                showActionChooserForElement = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🎨 Customize Appearance", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showActionChooserForElement = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Text("Cancel", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Horizontal Pager across launcher schema pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex)
            if (page != null) {
                // Flex & Grid container with proper padding & margin scopes
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showHomescreenOptions = true }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Page Title Header Span
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = page.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }

                    // Render grid widget items with flexible span management
                    items(
                        items = page.elements,
                        key = { it.element_id },
                        span = { elem ->
                            val defaultSpan = when (elem.type) {
                                ElementType.CLOCK,
                                ElementType.WEATHER,
                                ElementType.AGENT_WIDGET,
                                ElementType.MEDIA_PLAYER,
                                ElementType.PIUU_SUITE_FOLDER,
                                ElementType.NOTIFICATION_CENTER,
                                ElementType.CUSTOM_CARD -> 2
                                else -> 1
                            }
                            val spanWidth = if ((elem.style_props.w ?: if (defaultSpan == 2) 4 else 2) >= 3) 2 else 1
                            GridItemSpan(spanWidth)
                        }
                    ) { elem ->
                        val isResizingThis = activeResizingElementId == elem.element_id
                        val borderAccent = if (isResizingThis) {
                            parseHexColor(elem.style_props.accentColor ?: "", PrimaryBlue)
                        } else {
                            Color.Transparent
                        }

                        val widgetHeight = when (elem.style_props.h ?: 2) {
                            1 -> 95.dp
                            2 -> 145.dp
                            3 -> 215.dp
                            4 -> 285.dp
                            else -> 145.dp
                        }

                        val isHovered = hoveredElementId == elem.element_id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(widgetHeight)
                                .alpha(if (draggedElement?.element_id == elem.element_id) 0.2f else 1.0f)
                                .then(
                                    if (isResizingThis) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = borderAccent,
                                            shape = RoundedCornerShape((elem.style_props.borderRadius ?: 24).dp)
                                        )
                                    } else if (isHovered) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = AccentPurple,
                                            shape = RoundedCornerShape((elem.style_props.borderRadius ?: 24).dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (activeResizingElementId != null) {
                                            activeResizingElementId = if (isResizingThis) null else elem.element_id
                                        }
                                    },
                                    onLongClick = {
                                        showActionChooserForElement = elem
                                    }
                                )
                                .onGloballyPositioned { coords ->
                                    elementBounds[elem.element_id] = coords.boundsInWindow()
                                }
                                .pointerInput(elem.element_id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { startOffset ->
                                            val bounds = elementBounds[elem.element_id]
                                            if (bounds != null) {
                                                draggedElement = elem
                                                draggedOffset = androidx.compose.ui.geometry.Offset.Zero
                                                currentTouchPosition = bounds.topLeft + startOffset
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedOffset += dragAmount
                                            currentTouchPosition += dragAmount
                                            
                                            val hovered = elementBounds.entries.find { (id, rect) ->
                                                id != elem.element_id && rect.contains(currentTouchPosition)
                                            }?.key
                                            hoveredElementId = hovered
                                        },
                                        onDragEnd = {
                                            val sourceElem = draggedElement
                                            val targetElemId = hoveredElementId
                                            if (sourceElem != null && targetElemId != null) {
                                                val targetElem = page.elements.find { it.element_id == targetElemId }
                                                if (targetElem != null) {
                                                    val sourceIsApp = sourceElem.type == ElementType.APP_GRID && sourceElem.action_intent?.type == "launch_app"
                                                    val targetIsApp = targetElem.type == ElementType.APP_GRID && targetElem.action_intent?.type == "launch_app"
                                                    val targetIsFolder = targetElem.type == ElementType.PIUU_SUITE_FOLDER && targetElem.data_props?.containsKey("is_custom_folder") == true
                                                    
                                                    if (sourceIsApp && (targetIsApp || targetIsFolder)) {
                                                        val elementsList = page.elements.toMutableList()
                                                        elementsList.remove(sourceElem)
                                                        
                                                        if (targetIsApp) {
                                                            val folderId = "custom_folder_${System.currentTimeMillis()}"
                                                            val sourceAppPkg = sourceElem.action_intent?.target ?: ""
                                                            val targetAppPkg = targetElem.action_intent?.target ?: ""
                                                            val sourceAppName = sourceElem.title ?: "App"
                                                            val targetAppName = targetElem.title ?: "App"
                                                            
                                                            val newFolder = LauncherElement(
                                                                element_id = folderId,
                                                                type = ElementType.PIUU_SUITE_FOLDER,
                                                                title = "Custom Folder",
                                                                style_props = StyleProps(w = 2, h = 2, borderRadius = 20, accentColor = "#8B5CF6"),
                                                                data_props = mapOf(
                                                                    "is_custom_folder" to true,
                                                                    "app_packages" to listOf(targetAppPkg, sourceAppPkg),
                                                                    "app_names" to listOf(targetAppName, sourceAppName)
                                                                )
                                                            )
                                                            val tgtIdx = elementsList.indexOfFirst { it.element_id == targetElem.element_id }
                                                            if (tgtIdx != -1) {
                                                                elementsList[tgtIdx] = newFolder
                                                            }
                                                        } else {
                                                            val sourceAppPkg = sourceElem.action_intent?.target ?: ""
                                                            val sourceAppName = sourceElem.title ?: "App"
                                                            
                                                            val existingPackages = (targetElem.data_props?.get("app_packages") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                                                            val existingNames = (targetElem.data_props?.get("app_names") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                                                            
                                                            if (!existingPackages.contains(sourceAppPkg)) {
                                                                existingPackages.add(sourceAppPkg)
                                                                existingNames.add(sourceAppName)
                                                            }
                                                            
                                                            val updatedFolder = targetElem.copy(
                                                                data_props = targetElem.data_props?.toMutableMap()?.apply {
                                                                    put("app_packages", existingPackages)
                                                                    put("app_names", existingNames)
                                                                } ?: mapOf(
                                                                    "is_custom_folder" to true,
                                                                    "app_packages" to existingPackages,
                                                                    "app_names" to existingNames
                                                                )
                                                            )
                                                            val tgtIdx = elementsList.indexOfFirst { it.element_id == targetElem.element_id }
                                                            if (tgtIdx != -1) {
                                                                elementsList[tgtIdx] = updatedFolder
                                                            }
                                                        }
                                                        
                                                        val updatedPages = pages.mapIndexed { idx, p ->
                                                            if (idx == pageIndex) p.copy(elements = elementsList) else p
                                                        }
                                                        onSaveSchema(schema.copy(pages = updatedPages))
                                                        Toast.makeText(context, "Created custom folder", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val elementsList = page.elements.toMutableList()
                                                        val srcIdx = elementsList.indexOfFirst { it.element_id == sourceElem.element_id }
                                                        val tgtIdx = elementsList.indexOfFirst { it.element_id == targetElem.element_id }
                                                        if (srcIdx != -1 && tgtIdx != -1) {
                                                            elementsList.removeAt(srcIdx)
                                                            elementsList.add(tgtIdx, sourceElem)
                                                            val updatedPages = pages.mapIndexed { idx, p ->
                                                                if (idx == pageIndex) p.copy(elements = elementsList) else p
                                                            }
                                                            onSaveSchema(schema.copy(pages = updatedPages))
                                                            Toast.makeText(context, "Rearranged homescreen grid", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                            draggedElement = null
                                            hoveredElementId = null
                                        },
                                        onDragCancel = {
                                            draggedElement = null
                                            hoveredElementId = null
                                        }
                                    )
                                }
                        ) {
                            // Widget rendering wrapper to fill max size
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (elem.type) {
                                    ElementType.CLOCK -> ClockWidgetComposable(
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.WEATHER -> WeatherWidgetComposable(
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.BATTERY_STATUS -> BatteryWidgetComposable(
                                        metrics = metrics,
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.SYSTEM_STATS -> SystemStatsWidgetComposable(
                                        metrics = metrics,
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.MEDIA_PLAYER -> MediaPlayerWidgetComposable(
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.AGENT_WIDGET -> AiInsightWidgetComposable(
                                        onOpenBrainChat = onOpenBrainChat,
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.QUICK_NOTES -> QuickNotesWidgetComposable(
                                        notes = notes,
                                        onAddNote = onAddNote,
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.PIUU_SUITE_FOLDER -> {
                                        if (elem.data_props?.get("is_custom_folder") == true) {
                                            CustomFolderWidgetComposable(
                                                folder = elem,
                                                onClick = { activeFolderToShow = elem },
                                                styleProps = elem.style_props
                                            )
                                        } else {
                                            PiuuSuiteFolderWidgetComposable(
                                                onOpenFolder = onOpenFolder,
                                                title = elem.title,
                                                styleProps = elem.style_props
                                            )
                                        }
                                    }
                                    ElementType.PREDICTIVE_SUGGESTIONS -> PredictiveSuggestionsWidgetComposable(
                                        apps = installedApps,
                                        onLaunchApp = onLaunchApp,
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
                                    ElementType.NOTIFICATION_CENTER -> {
                                        NotificationCenterWidgetComposable(
                                            notifications = notifications,
                                            onDismissNotification = { notificationBridge.dismissNotification(it) },
                                            onClearAll = { notificationBridge.clearAllNotifications() },
                                            title = elem.title,
                                            styleProps = elem.style_props
                                        )
                                    }
                                    ElementType.APP_GRID -> {
                                        if (elem.action_intent?.type == "launch_app") {
                                            val packageName = elem.action_intent.target
                                            val app = installedApps.find { it.package_name == packageName }
                                            val iconName = app?.icon_name ?: "apps"
                                            val appName = app?.name ?: elem.title ?: "App"

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .combinedClickable(
                                                        onClick = {
                                                            app?.let { onLaunchApp(it) } ?: run {
                                                                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                                                if (intent != null) {
                                                                    context.startActivity(intent)
                                                                } else {
                                                                    Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        },
                                                        onLongClick = {
                                                            app?.let { onLongPressApp(it) }
                                                        }
                                                    )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = getAppIconVector(iconName),
                                                        contentDescription = appName,
                                                        tint = PrimaryBlue,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = appName,
                                                    fontSize = 11.sp,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, LauncherBorder, RoundedCornerShape(24.dp))
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Frequent Apps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceAround
                                                    ) {
                                                        installedApps.take(4).forEach { app ->
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                modifier = Modifier.combinedClickable(
                                                                    onClick = { onLaunchApp(app) },
                                                                    onLongClick = { onLongPressApp(app) }
                                                                )
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(44.dp)
                                                                        .clip(CircleShape)
                                                                        .background(PrimaryBlue.copy(alpha = 0.2f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = getAppIconVector(app.icon_name),
                                                                        contentDescription = app.name,
                                                                        tint = PrimaryBlue
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(
                                                                    text = app.name,
                                                                    fontSize = 11.sp,
                                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                                                    lineHeight = 16.sp,
                                                                    letterSpacing = 0.5.sp,
                                                                    color = TextSecondary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CardGlassBg),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = elem.title ?: "Launcher Widget",
                                                modifier = Modifier.padding(16.dp),
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // RESIZING HANDLES (overlay if isResizingThis is true)
                            if (isResizingThis) {
                                // Dim/Tint overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.05f))
                                )

                                // Width Handle (Right edge button to toggle 1-col / 2-col span)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val currentW = elem.style_props.w ?: 2
                                            val newW = if (currentW >= 3) 2 else 4
                                            val updatedProps = elem.style_props.copy(w = newW)
                                            val updatedElem = elem.copy(style_props = updatedProps)
                                            val updatedPages = pages.map { page ->
                                                val updatedElements = page.elements.map { e ->
                                                    if (e.element_id == elem.element_id) updatedElem else e
                                                }
                                                page.copy(elements = updatedElements)
                                            }
                                            onSaveSchema(schema.copy(pages = updatedPages))
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = borderAccent,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if ((elem.style_props.w ?: 2) >= 3) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Resize Width",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Height handles row at the bottom edge
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentH = elem.style_props.h ?: 2
                                    IconButton(
                                        onClick = {
                                            if (currentH > 1) {
                                                val newH = currentH - 1
                                                val updatedProps = elem.style_props.copy(h = newH)
                                                val updatedElem = elem.copy(style_props = updatedProps)
                                                val updatedPages = pages.map { page ->
                                                    val updatedElements = page.elements.map { e ->
                                                        if (e.element_id == elem.element_id) updatedElem else e
                                                    }
                                                    page.copy(elements = updatedElements)
                                                }
                                                onSaveSchema(schema.copy(pages = updatedPages))
                                            }
                                        },
                                        enabled = currentH > 1,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (currentH > 1) borderAccent else Color(0x33FFFFFF),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Reduce Height",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(borderAccent.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "H: $currentH",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (currentH < 4) {
                                                val newH = currentH + 1
                                                val updatedProps = elem.style_props.copy(h = newH)
                                                val updatedElem = elem.copy(style_props = updatedProps)
                                                val updatedPages = pages.map { page ->
                                                    val updatedElements = page.elements.map { e ->
                                                        if (e.element_id == elem.element_id) updatedElem else e
                                                    }
                                                    page.copy(elements = updatedElements)
                                                }
                                                onSaveSchema(schema.copy(pages = updatedPages))
                                            }
                                        },
                                        enabled = currentH < 4,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (currentH < 4) borderAccent else Color(0x33FFFFFF),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Increase Height",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Page Indicator Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) PrimaryBlue else TextMuted.copy(alpha = 0.4f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 6.dp)
                )
            }
        }

        // Dock Bar
        DockBar(
            dockApps = dockApps,
            onLaunchApp = onLaunchApp,
            onOpenDrawer = onOpenDrawer,
            onLongPressApp = onLongPressApp,
            dockBackgroundStyle = prefManager.dockBackgroundStyle,
            dockShowLabels = prefManager.dockShowLabels,
            dockVisible = prefManager.dockVisible
        )
    } // End of Column

    // Custom folder dialog popup
    if (activeFolderToShow != null) {
        val folder = activeFolderToShow!!
        val appPackages = (folder.data_props?.get("app_packages") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val appNames = (folder.data_props?.get("app_names") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val folderTitle = folder.title ?: "Folder"
        
        Dialog(onDismissRequest = { activeFolderToShow = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CardGlassBg.copy(alpha = 0.98f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, LauncherBorder),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var isEditingName by remember { mutableStateOf(false) }
                        var editNameText by remember { mutableStateOf(folderTitle) }
                        
                        if (isEditingName) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = editNameText,
                                    onValueChange = { editNameText = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = LauncherBorder,
                                        focusedContainerColor = Color(0x11FFFFFF),
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        isEditingName = false
                                        val updatedFolder = folder.copy(title = editNameText)
                                        val updatedPages = pages.map { page ->
                                            val updatedElements = page.elements.map { e ->
                                                if (e.element_id == folder.element_id) updatedFolder else e
                                            }
                                            page.copy(elements = updatedElements)
                                        }
                                        onSaveSchema(schema.copy(pages = updatedPages))
                                        activeFolderToShow = updatedFolder
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = SuccessGreen)
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = folderTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { isEditingName = true }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        
                        IconButton(onClick = { activeFolderToShow = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        appPackages.forEachIndexed { index, pkg ->
                            val name = appNames.getOrNull(index) ?: "App"
                            val systemApp = installedApps.find { it.package_name == pkg }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                    .border(1.dp, LauncherBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        activeFolderToShow = null
                                        if (systemApp != null) {
                                            onLaunchApp(systemApp)
                                        } else {
                                            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getAppIconVector(mapPkgToIcon(pkg)),
                                            contentDescription = name,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                                
                                IconButton(
                                    onClick = {
                                        val currentPackages = appPackages.toMutableList()
                                        val currentNames = appNames.toMutableList()
                                        currentPackages.remove(pkg)
                                        currentNames.remove(name)
                                        
                                        val newAppElem = LauncherElement(
                                            element_id = "elem_${System.currentTimeMillis()}",
                                            type = ElementType.APP_GRID,
                                            title = name,
                                            style_props = StyleProps(w = 1, h = 1),
                                            action_intent = ActionIntent(type = "launch_app", target = pkg)
                                        )
                                        
                                        val pageObj = pages.getOrNull(pagerState.currentPage)
                                        if (pageObj != null) {
                                            val elementsList = pageObj.elements.toMutableList()
                                            elementsList.add(newAppElem)
                                            
                                            if (currentPackages.isEmpty()) {
                                                elementsList.remove(folder)
                                                activeFolderToShow = null
                                                Toast.makeText(context, "Removed $name and dissolved folder", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val updatedFolder = folder.copy(
                                                    data_props = folder.data_props?.toMutableMap()?.apply {
                                                        put("app_packages", currentPackages)
                                                        put("app_names", currentNames)
                                                    } ?: mapOf(
                                                        "is_custom_folder" to true,
                                                        "app_packages" to currentPackages,
                                                        "app_names" to currentNames
                                                    )
                                                )
                                                val fIdx = elementsList.indexOfFirst { it.element_id == folder.element_id }
                                                if (fIdx != -1) {
                                                    elementsList[fIdx] = updatedFolder
                                                }
                                                activeFolderToShow = updatedFolder
                                                Toast.makeText(context, "Removed $name from folder", Toast.LENGTH_SHORT).show()
                                            }
                                            
                                            val updatedPages = pages.mapIndexed { idx, p ->
                                                if (idx == pagerState.currentPage) p.copy(elements = elementsList) else p
                                            }
                                            onSaveSchema(schema.copy(pages = updatedPages))
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove App", tint = DangerRed.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val pageObj = pages.getOrNull(pagerState.currentPage)
                                if (pageObj != null) {
                                    val elementsList = pageObj.elements.toMutableList()
                                    elementsList.remove(folder)
                                    
                                    appPackages.forEachIndexed { idx, pkg ->
                                        val name = appNames.getOrNull(idx) ?: "App"
                                        val newAppElem = LauncherElement(
                                            element_id = "elem_${System.currentTimeMillis() + idx}",
                                            type = ElementType.APP_GRID,
                                            title = name,
                                            style_props = StyleProps(w = 1, h = 1),
                                            action_intent = ActionIntent(type = "launch_app", target = pkg)
                                        )
                                        elementsList.add(newAppElem)
                                    }
                                    
                                    val updatedPages = pages.mapIndexed { idx, p ->
                                        if (idx == pagerState.currentPage) p.copy(elements = elementsList) else p
                                    }
                                    onSaveSchema(schema.copy(pages = updatedPages))
                                    activeFolderToShow = null
                                    Toast.makeText(context, "Folder dissolved, apps placed on homescreen", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.15f), contentColor = DangerRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dissolve Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { activeFolderToShow = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Text("Close", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }

    // Floating drag overlay
    if (draggedElement != null && parentBounds != null) {
        val elem = draggedElement!!
        val bounds = elementBounds[elem.element_id]
        val pBounds = parentBounds!!
        if (bounds != null) {
            val localX = bounds.left - pBounds.left + draggedOffset.x
            val localY = bounds.top - pBounds.top + draggedOffset.y
            
            val widgetHeight = when (elem.style_props.h ?: 2) {
                1 -> 95.dp
                2 -> 145.dp
                3 -> 215.dp
                4 -> 285.dp
                else -> 145.dp
            }
            
            val density = LocalDensity.current.density
            
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        androidx.compose.ui.unit.IntOffset(
                            localX.toInt(),
                            localY.toInt()
                        )
                    }
                    .width((bounds.width / density).dp)
                    .height(widgetHeight)
                    .alpha(0.85f)
                    .shadow(12.dp, RoundedCornerShape((elem.style_props.borderRadius ?: 24).dp))
                    .background(CardGlassBg.copy(alpha = 0.95f), RoundedCornerShape((elem.style_props.borderRadius ?: 24).dp))
                    .border(2.dp, AccentPurple, RoundedCornerShape((elem.style_props.borderRadius ?: 24).dp)),
                contentAlignment = Alignment.Center
            ) {
                val isApp = elem.type == ElementType.APP_GRID && elem.action_intent?.type == "launch_app"
                val isCustomFolder = elem.type == ElementType.PIUU_SUITE_FOLDER && elem.data_props?.containsKey("is_custom_folder") == true
                
                if (isApp) {
                    val packageName = elem.action_intent?.target ?: ""
                    val app = installedApps.find { it.package_name == packageName }
                    val iconName = app?.icon_name ?: "apps"
                    val appName = app?.name ?: elem.title ?: "App"
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getAppIconVector(iconName),
                                contentDescription = appName,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = appName,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (isCustomFolder) {
                    val appPackages = (elem.data_props?.get("app_packages") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        CustomFolderPreviewGrid(appPackages = appPackages)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = elem.title ?: "Folder",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = elem.title ?: "Widget",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
} // End of outer Box container
} // End of HomeScreen function



@Composable
fun WidgetDashboardItemRow(
    name: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAdd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LauncherBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

fun mapPkgToIcon(pkg: String): String {
    return when {
        pkg.contains("phone") -> "phone"
        pkg.contains("mms") || pkg.contains("message") -> "message"
        pkg.contains("chrome") || pkg.contains("browser") -> "globe"
        pkg.contains("camera") -> "camera"
        pkg.contains("brain") -> "brain"
        pkg.contains("folder") -> "folder"
        pkg.contains("code") || pkg.contains("ide") -> "code"
        pkg.contains("music") || pkg.contains("spotify") -> "music"
        pkg.contains("mail") || pkg.contains("gmail") -> "mail"
        pkg.contains("video") || pkg.contains("youtube") -> "video"
        pkg.contains("calendar") -> "calendar"
        pkg.contains("settings") -> "settings"
        pkg.contains("note") -> "note"
        pkg.contains("calculator") -> "calculator"
        pkg.contains("cloud") -> "cloud"
        pkg.contains("file") -> "file"
        pkg.contains("clock") -> "clock"
        else -> "apps"
    }
}

@Composable
fun CustomFolderPreviewGrid(appPackages: List<String>) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        val displayPackages = appPackages.take(4)
        Column(
            modifier = Modifier.padding(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val pkg1 = displayPackages.getOrNull(0)
                if (pkg1 != null) {
                    Icon(
                        imageVector = getAppIconVector(mapPkgToIcon(pkg1)),
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
                val pkg2 = displayPackages.getOrNull(1)
                if (pkg2 != null) {
                    Icon(
                        imageVector = getAppIconVector(mapPkgToIcon(pkg2)),
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val pkg3 = displayPackages.getOrNull(2)
                if (pkg3 != null) {
                    Icon(
                        imageVector = getAppIconVector(mapPkgToIcon(pkg3)),
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(14.dp)
                    )
                }
                val pkg4 = displayPackages.getOrNull(3)
                if (pkg4 != null) {
                    Icon(
                        imageVector = getAppIconVector(mapPkgToIcon(pkg4)),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomFolderWidgetComposable(
    folder: LauncherElement,
    onClick: () -> Unit,
    styleProps: StyleProps = StyleProps()
) {
    val cardShape = RoundedCornerShape((styleProps.borderRadius ?: 24).dp)
    val accentColor = if (!styleProps.accentColor.isNullOrBlank()) parseHexColor(styleProps.accentColor, AccentPurple) else AccentPurple
    val bgOpacity = styleProps.opacity ?: 0.2f
    val borderCol = if (styleProps.borderStyle == "none") Color.Transparent else accentColor.copy(alpha = 0.5f)
    val appPackages = (folder.data_props?.get("app_packages") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    val folderTitle = folder.title ?: "Folder"

    Card(
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = bgOpacity)),
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, cardShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomFolderPreviewGrid(appPackages = appPackages)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${appPackages.size} apps inside",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        }
    }
}

