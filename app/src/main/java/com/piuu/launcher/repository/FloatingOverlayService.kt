package com.piuu.launcher.repository

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.piuu.launcher.model.SystemApp
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class FloatingLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatView: View? = null
    private var dismissView: View? = null

    private var params: WindowManager.LayoutParams? = null
    private var dismissParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var isExpanded = false
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        createDismissZoneWindow()
        createFloatingOverlayWindow()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "piuu_pip_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Piuu Floating Assist",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Piuu Side Edge Assist Active")
                .setContentText("Tap or swipe edge to open Quick Notes & App Switcher")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Piuu Side Edge Assist Active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }

        startForeground(9901, notification)
    }

    private fun createDismissZoneWindow() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        dismissParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (120 * resources.displayMetrics.density).toInt(),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        val lifecycleOwner = FloatingLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)

        dismissView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                DismissZoneComposable()
            }
        }
        dismissView?.visibility = View.GONE
        windowManager.addView(dismissView, dismissParams)
    }

    private fun createFloatingOverlayWindow() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val configManager = LauncherConfigManager.getInstance(this)
        val config = configManager.config
        val gravitySide = if (config.pipEdgeSide == "left") Gravity.LEFT else Gravity.RIGHT

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or gravitySide
            x = 0
            y = 300
        }

        val lifecycleOwner = FloatingLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)

        floatView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                var expanded by remember { mutableStateOf(false) }

                LaunchedEffect(expanded) {
                    isExpanded = expanded
                    updateWindowDimensions(expanded)
                }

                Box(modifier = Modifier.wrapContentSize()) {
                    if (!expanded) {
                        SideEdgePillComposable(
                            onExpand = { expanded = true }
                        )
                    } else {
                        ExpandedPanelComposable(
                            onMinimize = { expanded = false },
                            onCloseService = { stopSelf() }
                        )
                    }
                }
            }
        }

        floatView?.setOnTouchListener { _, event ->
            if (isExpanded) return@setOnTouchListener false

            val currentParams = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = currentParams.x
                    initialY = currentParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        dismissView?.visibility = View.VISIBLE
                    }

                    currentParams.x = initialX + dx
                    currentParams.y = initialY + dy
                    windowManager.updateViewLayout(floatView, currentParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    dismissView?.visibility = View.GONE

                    if (isDragging && currentParams.y < 150) {
                        stopSelf()
                        return@setOnTouchListener true
                    }
                    false
                }
                else -> false
            }
        }

        windowManager.addView(floatView, params)
    }

    private fun updateWindowDimensions(expanded: Boolean) {
        val currentParams = params ?: return
        if (expanded) {
            currentParams.width = 340.dp.toPx(this)
            currentParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            currentParams.flags = currentParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            currentParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            currentParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            currentParams.flags = currentParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(floatView, currentParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatView != null) {
            windowManager.removeView(floatView)
            floatView = null
        }
        if (dismissView != null) {
            windowManager.removeView(dismissView)
            dismissView = null
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }
}

@Composable
fun DismissZoneComposable() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xDD090D1A), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF3B30))
                    .border(1.5.dp, Color(0xFFFF3B30), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "🗑️ Drop to remove PiP",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SideEdgePillComposable(onExpand: () -> Unit) {
    val context = LocalContext.current
    val configManager = remember { LauncherConfigManager.getInstance(context) }
    val config = configManager.config

    Box(
        modifier = Modifier
            .size(width = config.pipBarWidth.dp, height = config.pipBarHeight.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Color(0xCC00C6FF), Color(0xCC0072FF))))
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onExpand() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = "Expand Side Assist",
            tint = Color.White,
            modifier = Modifier.size(config.pipIconSize.dp)
        )
    }
}

@Composable
fun ExpandedPanelComposable(
    onMinimize: () -> Unit,
    onCloseService: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Notes, 1 = Quick Switcher, 2 = System Telemetry, 3 = PiP Settings

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x223B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Piuu Side Assist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMinimize, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Minimize, contentDescription = "Minimize", tint = Color(0xAAFFFFFF), modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onCloseService, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFFF453A), modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0FFFFFFF), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TabButton("Notes", Icons.Default.EditNote, activeTab == 0, { activeTab = 0 }, Modifier.weight(1f))
                TabButton("Apps", Icons.Default.Apps, activeTab == 1, { activeTab = 1 }, Modifier.weight(1f))
                TabButton("System", Icons.Default.Memory, activeTab == 2, { activeTab = 2 }, Modifier.weight(1f))
                TabButton("Settings", Icons.Default.Settings, activeTab == 3, { activeTab = 3 }, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content Area based on Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    0 -> NotesPipContent()
                    1 -> QuickAppSwitcherContent(onLaunch = onMinimize)
                    2 -> SystemTelemetryContent()
                    3 -> PipSettingsContent()
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF3B82F6) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else Color(0xAAFFFFFF),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = if (selected) Color.White else Color(0xAAFFFFFF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NotesPipContent() {
    val context = LocalContext.current
    val repository = remember { NotesRepository.getInstance(context) }
    val notes by repository.notes.collectAsState()

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0x11FFFFFF), RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "📝 Persistent Notes (${notes.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(notes) { note ->
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(note.content, fontSize = 9.sp, color = Color(0xAAFFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { repository.deleteNote(note.id) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = Color(0xFFFF453A), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    placeholder = { Text("Title", fontSize = 9.sp, color = Color(0x66FFFFFF)) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    placeholder = { Text("Content...", fontSize = 9.sp, color = Color(0x66FFFFFF)) },
                    modifier = Modifier.weight(2f).height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank() || noteContent.isNotBlank()) {
                            repository.addNote(
                                title = if (noteTitle.isBlank()) "Quick Note" else noteTitle,
                                content = noteContent
                            )
                            noteTitle = ""
                            noteContent = ""
                        }
                    },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("+", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickAppSwitcherContent(onLaunch: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { LauncherRepository(context) }
    val apps = remember { repository.defaultApps.take(8) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("⚡ Quick App Access", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(apps) { app ->
                Card(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.package_name)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                            onLaunch()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(app.name.take(1), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(app.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun SystemTelemetryContent() {
    val cpuLoad by LibC.systemCpuLoad.collectAsState()
    val memInfo = remember { LibC.getMemInfo() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📊 Native System Telemetry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text("CPU Load: ${String.format("%.1f", cpuLoad)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C6FF))
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (cpuLoad.toFloat() / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00C6FF),
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }

        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text("RAM Usage: ${String.format("%.1f", memInfo.usedGb)} GB / ${String.format("%.1f", memInfo.totalGb)} GB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38EF7D))
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (memInfo.usedGb / memInfo.totalGb).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF38EF7D),
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }
    }
}

@Composable
fun PipSettingsContent() {
    val context = LocalContext.current
    val configManager = remember { LauncherConfigManager.getInstance(context) }
    var config by remember { mutableStateOf(configManager.config) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("⚙️ PiP Side Edge Configuration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Side Position", fontSize = 11.sp, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = config.pipEdgeSide == "left",
                    onClick = {
                        val newConfig = config.copy(pipEdgeSide = "left")
                        configManager.config = newConfig
                        config = newConfig
                    },
                    label = { Text("Left", fontSize = 10.sp) }
                )
                FilterChip(
                    selected = config.pipEdgeSide == "right",
                    onClick = {
                        val newConfig = config.copy(pipEdgeSide = "right")
                        configManager.config = newConfig
                        config = newConfig
                    },
                    label = { Text("Right", fontSize = 10.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edge Bar Height", fontSize = 11.sp, color = Color.White)
            Slider(
                value = config.pipBarHeight.toFloat(),
                onValueChange = {
                    val newConfig = config.copy(pipBarHeight = it.toInt())
                    configManager.config = newConfig
                    config = newConfig
                },
                valueRange = 40f..120f,
                modifier = Modifier.width(140.dp)
            )
        }
    }
}
