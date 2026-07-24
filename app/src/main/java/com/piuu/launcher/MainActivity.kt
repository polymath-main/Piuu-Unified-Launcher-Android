package com.piuu.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.piuu.launcher.model.*
import com.piuu.launcher.repository.AiEngine
import com.piuu.launcher.repository.LatencyManager
import com.piuu.launcher.repository.LauncherRepository
import com.piuu.launcher.repository.NotificationBridge
import com.piuu.launcher.ui.components.*
import com.piuu.launcher.ui.theme.PiuuLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: LauncherRepository
    private val aiEngine = AiEngine()
    private val latencyManager = LatencyManager.getInstance()
    private var resetUiStateCallback: (() -> Unit)? = null

    @androidx.webkit.WebViewCompat.ExperimentalAsyncStartUp
    @android.annotation.SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // Configure hardware acceleration, window margins, and high refresh rate
        latencyManager.configureWindowForSmoothScrolling(this)

        // Set wallpaper flags to show live/system wallpaper behind the transparent window background
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        // Let the content layout in the display cutout/notch area so there are no black bands around the notch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        repository = LauncherRepository(applicationContext)

        // Pre-warm app launch intents asynchronously for ultra-low launch latency
        latencyManager.prewarmAppIntents(this, repository.getApps().map { it.package_name })

        // 1. WebKit/Chromium Startup Workload Offloading using startUpWebView API
        try {
            if (androidx.webkit.WebViewFeature.isFeatureSupported("START_UP_WEBVIEW")) {
                val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
                val config = androidx.webkit.WebViewStartUpConfig.Builder(executor).build()
                androidx.webkit.WebViewCompat.startUpWebView(
                    config,
                    object : androidx.webkit.WebViewCompat.WebViewStartUpCallback {
                        override fun onSuccess(result: androidx.webkit.WebViewStartUpResult) {
                            android.util.Log.d("PiuuStartup", "WebViewCompat startUpWebView initialized successfully in background thread.")
                        }
                    }
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PiuuStartup", "Exception in startUpWebView", e)
        }

        // 2. Offscreen WebView Warm-up Preloader initialization to achieve "instant" in-browser feel
        val startupScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        com.piuu.launcher.repository.WebViewPreloader.prewarm(
            applicationContext,
            repository,
            latencyManager,
            aiEngine,
            startupScope
        )

        setContent {
            PiuuLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF020817)
                ) {
                    LauncherAppMain(
                        repository = repository,
                        aiEngine = aiEngine,
                        latencyManager = latencyManager,
                        onRegisterResetCallback = { callback ->
                            resetUiStateCallback = callback
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // When user presses Home key, reset overlays to return directly to home workspace
        if (intent?.hasCategory(Intent.CATEGORY_HOME) == true) {
            resetUiStateCallback?.invoke()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.piuu.launcher.repository.WebViewPreloader.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherAppMain(
    repository: LauncherRepository,
    aiEngine: AiEngine,
    latencyManager: LatencyManager = remember { LatencyManager.getInstance() },
    onRegisterResetCallback: ((() -> Unit) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // ── Launcher Core State ───────────────────────────────────────────────────
    var schema by remember { mutableStateOf(repository.getSchema()) }
    var apps by remember { mutableStateOf(repository.getApps()) }
    var notes by remember { mutableStateOf(repository.getNotes()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val configManager = remember { com.piuu.launcher.repository.LauncherConfigManager.getInstance(context) }
    var configState by remember { mutableStateOf(configManager.config) }

    val notificationBridge = remember { NotificationBridge.getInstance(context) }
    val notifications by notificationBridge.notificationsFlow.collectAsState()
    val marketplaceCatalog = remember { repository.marketplaceCatalog }
    var metrics by remember { mutableStateOf(SystemMetrics()) }

    // ── AI Chat Messages State ────────────────────────────────────────────────
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "m1",
                sender = "ai",
                text = "Hello! I am Piuu Neural Brain. How can I assist with your launcher layout, apps, or system metrics today?",
                timestamp = "09:00 AM"
            )
        )
    }

    // ── Overlay / Sheet States ────────────────────────────────────────────────
    var showDrawer by remember { mutableStateOf(false) }
    var showBrainChat by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showMarketplace by remember { mutableStateOf(false) }
    var showMetrics by remember { mutableStateOf(false) }
    var showSchemaEditor by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }

    // Reset Callback for HOME key press
    LaunchedEffect(Unit) {
        onRegisterResetCallback?.invoke {
            showDrawer = false
            showBrainChat = false
            showSearch = false
            showMarketplace = false
            showMetrics = false
            showSchemaEditor = false
            showWebView = false
            showQuickSettings = false
        }
    }

    // Back Button Handler: Close open sheets or do nothing (stay on home launcher)
    val isAnyOverlayOpen = showDrawer || showBrainChat || showSearch || showMarketplace || showMetrics || showSchemaEditor || showWebView || showQuickSettings
    BackHandler(enabled = isAnyOverlayOpen) {
        showDrawer = false
        showBrainChat = false
        showSearch = false
        showMarketplace = false
        showMetrics = false
        showSchemaEditor = false
        showWebView = false
        showQuickSettings = false
    }

    // Launch App helper with LatencyManager ultra-low latency bridge
    val handleLaunchApp: (SystemApp) -> Unit = { app ->
        repository.recordAppLaunch(app.package_name)
        apps = repository.getApps()

        when (app.package_name) {
            "com.piuu.brain" -> {
                showBrainChat = true
            }
            "com.piuu.ide" -> {
                showSchemaEditor = true
            }
            "com.piuu.suite" -> {
                showMarketplace = true
            }
            "com.piuu.notes" -> {
                showBrainChat = true
            }
            else -> {
                val launched = latencyManager.launchAppFast(context, app.package_name)
                if (!launched) {
                    Toast.makeText(context, "Opening ${app.name}...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Main Launcher View Container
    PiuuLauncherTheme(theme = schema.theme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (configState.showSystemWallpaper) Color.Transparent else Color(0xFF020817))
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Status Bar & Quick Actions
            HeaderBar(
                metrics = metrics,
                onOpenSearch = { showSearch = true },
                onOpenBrainChat = { showBrainChat = true },
                onOpenMarketplace = { showMarketplace = true },
                onOpenMetrics = { showMetrics = true },
                onOpenSchemaEditor = { showSchemaEditor = true },
                onOpenWebView = { showWebView = true },
                onOpenQuickSettings = { showQuickSettings = true }
            )

            // Main Pager Home Screen
            HomeScreen(
                schema = schema,
                metrics = metrics,
                installedApps = apps,
                notes = notes,
                onLaunchApp = handleLaunchApp,
                onOpenDrawer = { showDrawer = true },
                onOpenSearch = { showSearch = true },
                onOpenBrainChat = { showBrainChat = true },
                onOpenQuickSettings = { showQuickSettings = true },
                onOpenWebView = { showWebView = true },
                onOpenMarketplace = { showMarketplace = true },
                onOpenMetrics = { showMetrics = true },
                onOpenSchemaEditor = { showSchemaEditor = true },
                onOpenFolder = { showDrawer = true },
                onAddNote = { text ->
                    val updated = notes.toMutableList().apply {
                        add(NoteItem("n_${System.currentTimeMillis()}", text, "Just now", "#3B82F6"))
                    }
                    notes = updated
                    repository.saveNotes(updated)
                }
            )
        }

        // ── Overlays & Bottom Sheets ─────────────────────────────────────────

        // App Drawer
        AppDrawer(
            visible = showDrawer,
            apps = apps,
            onDismiss = { showDrawer = false },
            onLaunchApp = { app ->
                showDrawer = false
                handleLaunchApp(app)
            },
            onToggleFavorite = { app ->
                val updated = apps.toMutableList()
                val idx = updated.indexOfFirst { it.package_name == app.package_name }
                if (idx != -1) {
                    updated[idx] = updated[idx].copy(is_favorite = !updated[idx].is_favorite)
                    apps = updated
                    repository.saveApps(updated)
                }
            }
        )

        // Brain AI Assistant Chat
        BrainChatSheet(
            visible = showBrainChat,
            chatMessages = chatMessages,
            onDismiss = { showBrainChat = false },
            onSendMessage = { userText ->
                val userMsg = ChatMessage("u_${System.currentTimeMillis()}", "user", userText, "Just now")
                chatMessages.add(userMsg)
                coroutineScope.launch {
                    val aiResp = aiEngine.generateResponse(userText, "User active screen: Home Page")
                    chatMessages.add(aiResp)
                }
            }
        )

        // Universal Search
        GlobalSearchModal(
            visible = showSearch,
            apps = apps,
            onDismiss = { showSearch = false },
            onLaunchApp = { app ->
                showSearch = false
                handleLaunchApp(app)
            }
        )

        // Marketplace Catalog
        MarketplaceModal(
            visible = showMarketplace,
            catalog = repository.getMarketplaceCatalog(schema),
            aiEngine = aiEngine,
            onDismiss = { showMarketplace = false },
            onApplyAiTheme = { aiTheme ->
                val updatedSchema = schema.copy(theme = aiTheme)
                schema = updatedSchema
                repository.saveSchema(updatedSchema)
                Toast.makeText(context, "AI Theme '${aiTheme.name}' Applied!", Toast.LENGTH_SHORT).show()
                showMarketplace = false
            },
            onInstallItem = { item ->
                val currentTheme = schema.theme
                var updatedTheme = currentTheme
                var updatedSchema = schema

                when (item.category.lowercase()) {
                    "theming", "themes" -> {
                        var primary = "#3B82F6"
                        var accent = "#8B5CF6"
                        var bg = "rgba(2, 8, 23, 0.85)"
                        var font = currentTheme.font_family

                        if (!item.payload.isNullOrBlank()) {
                            try {
                                val json = org.json.JSONObject(item.payload)
                                if (json.has("primary_blue")) primary = json.getString("primary_blue")
                                else if (json.has("primary_color")) primary = json.getString("primary_color")

                                if (json.has("accent_purple")) accent = json.getString("accent_purple")
                                else if (json.has("accent_color")) accent = json.getString("accent_color")

                                if (json.has("bg_color")) bg = json.getString("bg_color")
                                else if (json.has("bg_overlay")) bg = json.getString("bg_overlay")
                                else if (json.has("card_bg")) bg = json.getString("card_bg")

                                if (json.has("font_family")) font = json.getString("font_family")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            val tuple = when (item.id) {
                                "theme_nord" -> Triple("#3B82F6", "#8B5CF6", "rgba(2, 8, 23, 0.85)")
                                "theme_cyberpunk" -> Triple("#EC4899", "#06B6D4", "rgba(9, 9, 11, 0.9)")
                                "theme_slate" -> Triple("#64748B", "#94A3B8", "rgba(18, 18, 18, 0.92)")
                                "theme_sunset" -> Triple("#F97316", "#A855F7", "rgba(30, 17, 42, 0.88)")
                                else -> Triple("#3B82F6", "#8B5CF6", "rgba(2, 8, 23, 0.85)")
                            }
                            primary = tuple.first
                            accent = tuple.second
                            bg = tuple.third
                        }

                        updatedTheme = currentTheme.copy(
                            id = item.id,
                            name = item.name,
                            primary_color = primary,
                            accent_color = accent,
                            bg_overlay = bg,
                            font_family = font
                        )
                        updatedSchema = schema.copy(theme = updatedTheme)
                    }
                    "fonts" -> {
                        var fontName = item.id
                        if (!item.payload.isNullOrBlank()) {
                            try {
                                val json = org.json.JSONObject(item.payload)
                                if (json.has("font_family")) fontName = json.getString("font_family")
                            } catch (e: Exception) {
                                fontName = item.payload
                            }
                        }
                        updatedTheme = currentTheme.copy(font_family = fontName)
                        updatedSchema = schema.copy(theme = updatedTheme, active_font = fontName)
                    }
                    "layouts" -> {
                        var layoutName = item.id
                        if (!item.payload.isNullOrBlank()) {
                            try {
                                val json = org.json.JSONObject(item.payload)
                                if (json.has("layout_name")) layoutName = json.getString("layout_name")
                            } catch (e: Exception) {
                                layoutName = item.payload
                            }
                        }
                        updatedSchema = schema.copy(drawer_layout = layoutName)
                    }
                    "icons" -> {
                        var packName = item.id
                        if (!item.payload.isNullOrBlank()) {
                            try {
                                val json = org.json.JSONObject(item.payload)
                                if (json.has("icon_pack_name")) packName = json.getString("icon_pack_name")
                            } catch (e: Exception) {
                                packName = item.payload
                            }
                        }
                        updatedSchema = schema.copy(active_icon_pack = packName)
                    }
                    else -> {
                        val plugins = schema.installed_plugins.toMutableList()
                        if (!plugins.contains(item.id)) {
                            plugins.add(item.id)
                        }
                        updatedSchema = schema.copy(installed_plugins = plugins)
                    }
                }

                schema = updatedSchema
                repository.saveSchema(updatedSchema)
                Toast.makeText(context, "Applied ${item.name} Scheme!", Toast.LENGTH_SHORT).show()
                showMarketplace = false
            }
        )

        // Usage Metrics & System Performance Dashboard
        MetricsDashboardModal(
            visible = showMetrics,
            metrics = metrics,
            apps = apps,
            onDismiss = { showMetrics = false }
        )

        // Schema Editor IDE
        SchemaEditorModal(
            visible = showSchemaEditor,
            schema = schema,
            onDismiss = { showSchemaEditor = false },
            onSaveSchema = { newSchema ->
                schema = newSchema
                repository.saveSchema(newSchema)
                showSchemaEditor = false
                Toast.makeText(context, "Launcher Schema updated!", Toast.LENGTH_SHORT).show()
            },
            onResetSchema = {
                val reset = repository.resetSchema()
                schema = reset
                showSchemaEditor = false
                Toast.makeText(context, "Schema reset to default!", Toast.LENGTH_SHORT).show()
            }
        )

        // Hybrid WebView Drawer Sheet
        if (showWebView) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showWebView = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF020817)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { showWebView = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HybridWebViewComposable(
                            repository = repository,
                            latencyManager = latencyManager,
                            aiEngine = aiEngine,
                            onLaunchApp = { packageName, appName ->
                                val app = apps.firstOrNull { it.package_name == packageName }
                                if (app != null) {
                                    handleLaunchApp(app)
                                } else {
                                    when (packageName) {
                                        "com.piuu.brain" -> {
                                            showBrainChat = true
                                        }
                                        "com.piuu.ide" -> {
                                            showSchemaEditor = true
                                        }
                                        "com.piuu.suite" -> {
                                            showMarketplace = true
                                        }
                                        else -> {
                                            latencyManager.launchAppFast(context, packageName)
                                        }
                                    }
                                }
                                showWebView = false
                            }
                        )
                    }
                }
            }
        }

        // Control Center & Quick Settings Shade
        QuickSettingsShade(
            visible = showQuickSettings,
            metrics = metrics,
            notifications = notifications,
            showSystemWallpaper = configState.showSystemWallpaper,
            onToggleWallpaper = {
                val updatedVal = !configState.showSystemWallpaper
                configManager.setWallpaperEnabled(updatedVal)
                configState = configManager.config
                Toast.makeText(context, if (updatedVal) "System wallpaper enabled" else "System wallpaper disabled", Toast.LENGTH_SHORT).show()
            },
            onChangeWallpaper = {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                try {
                    context.startActivity(Intent.createChooser(intent, "Choose Wallpaper Source"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No system wallpaper app found", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showQuickSettings = false },
            onToggleWifi = {
                metrics = metrics.copy(wifi_enabled = !metrics.wifi_enabled)
            },
            onToggleBluetooth = {
                metrics = metrics.copy(bluetooth_enabled = !metrics.bluetooth_enabled)
            },
            onToggleFlashlight = {
                metrics = metrics.copy(flashlight_enabled = !metrics.flashlight_enabled)
            },
            onToggleBatterySaver = {
                metrics = metrics.copy(battery_saver = !metrics.battery_saver)
            },
            onToggleDnd = {
                metrics = metrics.copy(do_not_disturb = !metrics.do_not_disturb)
            },
            onClearNotifications = {
                notificationBridge.clearAllNotifications()
            },
            onScanApps = {
                val scanned = repository.scanAndSaveInstalledApps(context)
                if (scanned.isNotEmpty()) {
                    apps = scanned
                    Toast.makeText(context, "Successfully imported ${scanned.size} apps!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No apps found, keeping defaults", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    }
}
