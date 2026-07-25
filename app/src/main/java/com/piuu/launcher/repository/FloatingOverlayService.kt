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
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.SystemApp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*

class FloatingOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "FloatingOverlay"
        private const val CHANNEL_ID = "floating_overlay_channel"
        private const val NOTIFICATION_ID = 9182

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var headView: FrameLayout? = null
    private var expandedView: FrameLayout? = null
    private var dismissView: FrameLayout? = null

    // Window Layout Params
    private lateinit var headParams: WindowManager.LayoutParams
    private lateinit var expandedParams: WindowManager.LayoutParams
    private lateinit var dismissParams: WindowManager.LayoutParams

    // Drag / State tracking
    private var isExpanded = false
    private var screenWidth = 1080
    private var screenHeight = 2200

    // Lifecycle variables
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        updateScreenDimensions()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        setupDismissZone()
        setupFloatingHead()
        setupExpandedPanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateScreenDimensions() {
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Piuu Seamless PIP Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls the active Picture-In-Picture & Multi-Window controller overlay"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Piuu PIP Overlay Active")
            .setContentText("Tap to interact or drag to the bottom of the screen to close")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    // ── Floating Head (Compact View) ───────────────────────────────────────────
    private fun setupFloatingHead() {
        headView = FrameLayout(this)
        setViewTreeOwners(headView!!)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        headParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 220
            y = screenHeight / 3
        }

        val composeView = ComposeView(this).apply {
            setContent {
                FloatingHeadComposable(
                    onTap = { toggleExpand() },
                    onDragStart = { showDismissZone(true) },
                    onDrag = { dx, dy -> moveHead(dx, dy) },
                    onDragEnd = { snapAndCheckDismiss() }
                )
            }
        }

        headView!!.addView(composeView)
        windowManager.addView(headView, headParams)
    }

    // ── Expanded Panel ──────────────────────────────────────────────────────────
    private fun setupExpandedPanel() {
        expandedView = FrameLayout(this)
        setViewTreeOwners(expandedView!!)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        expandedParams = WindowManager.LayoutParams(
            (screenWidth * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        val composeView = ComposeView(this).apply {
            setContent {
                ExpandedPanelComposable(
                    onMinimize = { toggleExpand() },
                    onCloseService = { stopSelf() }
                )
            }
        }

        expandedView!!.addView(composeView)
    }

    // ── Dismiss Zone ────────────────────────────────────────────────────────────
    private fun setupDismissZone() {
        dismissView = FrameLayout(this)
        setViewTreeOwners(dismissView!!)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        dismissParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            240,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        val composeView = ComposeView(this).apply {
            setContent {
                DismissZoneComposable()
            }
        }

        dismissView!!.addView(composeView)
    }

    private fun setViewTreeOwners(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    private fun showDismissZone(show: Boolean) {
        if (show) {
            if (dismissView?.parent == null) {
                try {
                    windowManager.addView(dismissView, dismissParams)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to show dismiss zone", e)
                }
            }
        } else {
            if (dismissView?.parent != null) {
                try {
                    windowManager.removeView(dismissView)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove dismiss zone", e)
                }
            }
        }
    }

    private fun moveHead(dx: Float, dy: Float) {
        headParams.x += dx.toInt()
        headParams.y += dy.toInt()

        // Restrict within screen bounds
        if (headParams.x < -40) headParams.x = -40
        if (headParams.x > screenWidth - 140) headParams.x = screenWidth - 140
        if (headParams.y < 0) headParams.y = 0
        if (headParams.y > screenHeight - 200) headParams.y = screenHeight - 200

        try {
            windowManager.updateViewLayout(headView, headParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating head layout during drag", e)
        }
    }

    private fun snapAndCheckDismiss() {
        showDismissZone(false)

        // Check if head is inside the Dismiss Zone (bottom center of the screen)
        val dismissYThreshold = screenHeight - 420
        val centerLeftThreshold = (screenWidth / 2) - 180
        val centerRightThreshold = (screenWidth / 2) + 180

        if (headParams.y > dismissYThreshold && headParams.x in centerLeftThreshold..centerRightThreshold) {
            // Trigger feedback
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            } catch (e: Exception) {
                // Ignore vibration failure
            }
            stopSelf()
            return
        }

        // Otherwise snap to closest edge (left or right)
        val middle = screenWidth / 2
        val targetX = if (headParams.x + 80 < middle) -20 else screenWidth - 150

        // Smooth snapping animation
        scope.launch {
            val startX = headParams.x
            val deltaX = targetX - startX
            val steps = 10
            for (i in 1..steps) {
                headParams.x = startX + (deltaX * i / steps)
                try {
                    if (headView?.parent != null) {
                        windowManager.updateViewLayout(headView, headParams)
                    }
                } catch (e: Exception) {
                    break
                }
                delay(12)
            }
        }
    }

    private fun toggleExpand() {
        if (isExpanded) {
            // Minimize
            try {
                if (expandedView?.parent != null) {
                    windowManager.removeView(expandedView)
                }
                if (headView?.parent == null) {
                    windowManager.addView(headView, headParams)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error minimizing", e)
            }
            isExpanded = false
        } else {
            // Expand
            try {
                if (headView?.parent != null) {
                    windowManager.removeView(headView)
                }
                if (expandedView?.parent == null) {
                    windowManager.addView(expandedView, expandedParams)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error expanding", e)
            }
            isExpanded = true
        }
    }

    override fun onDestroy() {
        scope.cancel()
        try {
            if (headView?.parent != null) windowManager.removeView(headView)
            if (expandedView?.parent != null) windowManager.removeView(expandedView)
            if (dismissView?.parent != null) windowManager.removeView(dismissView)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleanup views", e)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}

// ── Compose Composables for Overlay Views ───────────────────────────────────

@Composable
fun FloatingHeadComposable(
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .padding(4.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3B82F6), // Bright neon blue core
                        Color(0xFF1E3A8A), // Deep indigo
                        Color(0xFF0F172A)  // Slate boundary
                    )
                )
            )
            .border(2.dp, Color(0xAA3B82F6), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    }
                )
            }
            .clickable {
                if (!isDragging) {
                    onTap()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PictureInPicture,
            contentDescription = "PIP Helper",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

        // Pulsing orbit ring effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x333B82F6),
                radius = size.minDimension / 2f + 2.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx())
            )
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
                    colors = listOf(Color.Transparent, Color(0xDD090D1A))
                )
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF3B30))
                    .border(1.5.dp, Color(0xFFFF3B30), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Drag here to remove PIP",
                color = Color(0xAAFFFFFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ExpandedPanelComposable(
    onMinimize: () -> Unit,
    onCloseService: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Music PIP, 1 = Video PIP, 2 = App Drawer PIP

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)), // Rich Slate Dark Glass
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x223B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Piuu Floating Space",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Minimize,
                            contentDescription = "Minimize",
                            tint = Color(0xAAFFFFFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onCloseService,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Floating Assist",
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0FFFFFFF), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    text = "Music PIP",
                    icon = Icons.Default.MusicNote,
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Video PIP",
                    icon = Icons.Default.PlayArrow,
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Multi-Window",
                    icon = Icons.Default.Apps,
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area based on Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    0 -> MusicPipContent()
                    1 -> VideoPipContent()
                    2 -> AppLauncherContent(onLaunch = onMinimize)
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
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else Color(0xAAFFFFFF),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (selected) Color.White else Color(0xAAFFFFFF)
            )
        }
    }
}

// ── Tab 1: Music PIP Player Content ──────────────────────────────────────────
@Composable
fun MusicPipContent() {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0.42f) }
    var currentSongIndex by remember { mutableStateOf(0) }

    val playlist = listOf(
        Pair("Synthwave Sunrise", "Neon Odyssey"),
        Pair("Midnight Cyber Echoes", "Looming Shadows"),
        Pair("Arctic Minimalist", "Glacier Code")
    )
    val song = playlist[currentSongIndex]

    // Simulate progress counting up
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(1000)
                progress += 0.005f
                if (progress >= 1.0f) {
                    progress = 0f
                    currentSongIndex = (currentSongIndex + 1) % playlist.size
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Record Disc / Metadata Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rotating disk mockup or simple artwork
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves
                Canvas(modifier = Modifier.size(48.dp)) {
                    drawCircle(color = Color(0x22FFFFFF), style = Stroke(width = 1.dp.toPx()))
                    drawCircle(color = Color(0x22FFFFFF), style = Stroke(width = 3.dp.toPx()))
                }
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.first,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.second,
                    fontSize = 12.sp,
                    color = Color(0x99FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Live Simulated Equalizer / Visualizer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val bars = 18
            for (i in 0 until bars) {
                // Make a reactive animated height
                var barHeight by remember { mutableStateOf(10f) }
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        while (true) {
                            barHeight = (10f + sin(System.currentTimeMillis().toDouble() / 150.0 + i * 1.5).toFloat() * 25f + (0..10).random().toFloat()).coerceIn(4f, 40f)
                            delay(90)
                        }
                    } else {
                        barHeight = 4f
                    }
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                            )
                        )
                )
            }
        }

        // Progress bar
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                color = Color(0xFF3B82F6),
                trackColor = Color(0x22FFFFFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val secTotal = 240
                val secCurr = (secTotal * progress).toInt()
                Text(
                    text = String.format("%02d:%02d", secCurr / 60, secCurr % 60),
                    fontSize = 10.sp,
                    color = Color(0x66FFFFFF)
                )
                Text(
                    text = String.format("%02d:%02d", secTotal / 60, secTotal % 60),
                    fontSize = 10.sp,
                    color = Color(0x66FFFFFF)
                )
            }
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    currentSongIndex = if (currentSongIndex - 1 < 0) playlist.size - 1 else currentSongIndex - 1
                    progress = 0f
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .clickable { isPlaying = !isPlaying },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = {
                    currentSongIndex = (currentSongIndex + 1) % playlist.size
                    progress = 0f
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
            }
        }
    }
}

// ── Tab 2: YouTube PIP Simulated Video Content ──────────────────────────────
@Composable
fun VideoPipContent() {
    var isVideoPlaying by remember { mutableStateOf(true) }
    var playheadSeconds by remember { mutableStateOf(15) }
    val durationSeconds = 345

    LaunchedEffect(isVideoPlaying) {
        if (isVideoPlaying) {
            while (true) {
                delay(1000)
                playheadSeconds = (playheadSeconds + 1) % durationSeconds
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Video Canvas Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Live vector rendering simulating a running sci-fi visualization / futuristic video stream
            val renderTicks = remember { Animatable(0f) }
            LaunchedEffect(isVideoPlaying) {
                if (isVideoPlaying) {
                    renderTicks.animateTo(
                        targetValue = 1000f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 20000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 3.2f

                // Draw orbiting digital rings
                drawCircle(
                    color = Color(0x3300D4FF),
                    radius = baseRadius + sin(renderTicks.value / 10f) * 15f,
                    style = Stroke(width = 2.dp.toPx())
                )

                drawCircle(
                    color = Color(0x22D946EF),
                    radius = baseRadius * 1.3f + sin(renderTicks.value / 8f + 2f) * 10f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Render dynamic vector lines representing waveform or 3D coordinate space
                val linesCount = 12
                val currentSecs = playheadSeconds.toFloat()
                for (i in 0 until linesCount) {
                    val angle = (i * (360f / linesCount)) * (Math.PI / 180.0)
                    val scaleOffset = sin(currentSecs / 5f + i) * 20f
                    val outerRadius = baseRadius * 1.2f + scaleOffset
                    val start = Offset(
                        (center.x + sin(angle).toFloat() * baseRadius),
                        (center.y + cos(angle).toFloat() * baseRadius)
                    )
                    val end = Offset(
                        (center.x + sin(angle).toFloat() * outerRadius),
                        (center.y + cos(angle).toFloat() * outerRadius)
                    )
                    drawLine(
                        color = if (i % 2 == 0) Color(0xAA00D4FF) else Color(0xAAD946EF),
                        start = start,
                        end = end,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Center glowing sphere
                drawCircle(
                    color = Color(0xFF00D4FF),
                    radius = 8.dp.toPx()
                )
            }

            // YouTube Style overlay watermarks
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Top Watermark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Badge(containerColor = Color(0xBBFF0000)) {
                        Text("LIVE PIP", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "1080p HD",
                        color = Color(0x88FFFFFF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Middle Mini Play HUD overlay
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { isVideoPlaying = !isVideoPlaying },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x77000000))
                    ) {
                        Icon(
                            imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar with timing
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val progress = playheadSeconds.toFloat() / durationSeconds.toFloat()

            Text(
                text = String.format("%d:%02d", playheadSeconds / 60, playheadSeconds % 60),
                fontSize = 10.sp,
                color = Color(0xAAFFFFFF)
            )

            LinearProgressIndicator(
                progress = { progress },
                color = Color(0xFFFF0000), // YouTube Red
                trackColor = Color(0x22FFFFFF),
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.dp))
            )

            Text(
                text = String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60),
                fontSize = 10.sp,
                color = Color(0xAAFFFFFF)
            )
        }
    }
}

// ── Tab 3: Multi-Window System App Launcher ─────────────────────────────────
@Composable
fun AppLauncherContent(onLaunch: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val apps = listOf(
        SystemApp("com.android.phone", "Phone", "phone", "system", "intent://phone", 0, 0, true, "#10B981", ""),
        SystemApp("com.android.mms", "Messages", "message", "social", "intent://messages", 0, 0, true, "#3B82F6", ""),
        SystemApp("com.android.chrome", "Chrome", "globe", "utilities", "intent://chrome", 0, 0, true, "#EF4444", ""),
        SystemApp("com.android.camera", "Camera", "camera", "creative", "intent://camera", 0, 0, true, "#8B5CF6", ""),
        SystemApp("com.android.settings", "Settings", "settings", "system", "intent://settings", 0, 0, false, "#64748B", ""),
        SystemApp("com.android.calculator2", "Calculator", "calculator", "utilities", "intent://calculator", 0, 0, false, "#0EA5E9", "")
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Launch apps instantly in separate task windows:",
            fontSize = 10.sp,
            color = Color(0xAAFFFFFF),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(apps) { app ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.package_name)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                onLaunch()
                            } else {
                                android.widget.Toast
                                    .makeText(context, "App ${app.name} is simulated", android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(app.accent_color)).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (app.icon_name) {
                                    "phone" -> Icons.Default.Phone
                                    "message" -> Icons.Default.Email
                                    "globe" -> Icons.Default.Language
                                    "camera" -> Icons.Default.CameraAlt
                                    "settings" -> Icons.Default.Settings
                                    else -> Icons.Default.Calculate
                                },
                                contentDescription = null,
                                tint = Color(android.graphics.Color.parseColor(app.accent_color)),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
