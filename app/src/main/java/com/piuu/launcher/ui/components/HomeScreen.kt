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
import com.piuu.launcher.ui.theme.*

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

        // Homescreen Long-Press Options Dialog
        if (showHomescreenOptions) {
            Dialog(onDismissRequest = { showHomescreenOptions = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = com.piuu.launcher.ui.theme.CardGlassBg.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Home Options",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Configure layouts, choose system wallpapers, and adjust system-wide settings.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        // Option 1: Choose System Wallpaper
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                val intent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER)
                                try {
                                    context.startActivity(android.content.Intent.createChooser(intent, "Choose Wallpaper Source"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No system wallpaper app found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set System Wallpaper", fontWeight = FontWeight.Bold)
                        }

                        // Option 2: Quick Settings
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                onOpenQuickSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launcher Preferences", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        // Option 3: Edit Widgets (Schema Editor)
                        Button(
                            onClick = {
                                showHomescreenOptions = false
                                onOpenSchemaEditor()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Widgets & Screens", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showHomescreenOptions = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LauncherBorder)
                        ) {
                            Text("Dismiss", color = TextPrimary)
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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(widgetHeight)
                                .then(
                                    if (isResizingThis) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = borderAccent,
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
                                    ElementType.PIUU_SUITE_FOLDER -> PiuuSuiteFolderWidgetComposable(
                                        onOpenFolder = onOpenFolder,
                                        title = elem.title,
                                        styleProps = elem.style_props
                                    )
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
                                                            Text(app.name, fontSize = 11.sp, color = TextSecondary)
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
    }
}
