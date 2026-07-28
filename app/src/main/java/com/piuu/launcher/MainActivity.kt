package com.piuu.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piuu.launcher.model.*
import com.piuu.launcher.repository.AiEngine
import com.piuu.launcher.repository.LatencyManager
import com.piuu.launcher.repository.LauncherRepository
import com.piuu.launcher.repository.NotificationBridge
import com.piuu.launcher.repository.FloatingOverlayService
import com.piuu.launcher.ui.components.*
import com.piuu.launcher.ui.theme.*
import com.piuu.launcher.repository.WallpaperHandler
import com.piuu.launcher.repository.LauncherPreferenceManager
import com.piuu.launcher.repository.LauncherConfigManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: LauncherRepository
    private val aiEngine = AiEngine()
    private val latencyManager = LatencyManager.getInstance()
    private var resetUiStateCallback: (() -> Unit)? = null

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

        // Register dynamic WallpaperColors listener to extract primary/secondary colors from the system wallpaper and apply dynamically
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            val wallpaperManager = android.app.WallpaperManager.getInstance(applicationContext)
            val wallpaperHandler = WallpaperHandler(applicationContext)
            val listener = android.app.WallpaperManager.OnColorsChangedListener { colors, which ->
                if (which and android.app.WallpaperManager.FLAG_SYSTEM != 0) {
                    val palette = wallpaperHandler.extractWallpaperPalette()
                    val currentSchema = repository.getSchema()
                    val updatedTheme = currentSchema.theme.copy(
                        primary_color = palette.primaryHex,
                        accent_color = palette.vibrantHex,
                        bg_overlay = palette.bgOverlayHex
                    )
                    repository.saveSchema(currentSchema.copy(theme = updatedTheme))
                    runOnUiThread {
                        resetUiStateCallback?.invoke()
                    }
                }
            }
            try {
                wallpaperManager.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Pre-warm app launch intents asynchronously for ultra-low launch latency
        latencyManager.prewarmAppIntents(this, repository.getApps().map { it.package_name })

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
            val isDark = prefManager.isDarkMode
            isDarkMode = isDark

            PiuuLauncherTheme(isDark = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LauncherBackground
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
    var hiddenApps by remember { mutableStateOf(repository.getHiddenApps()) }
    val visibleApps = remember(apps, hiddenApps) { apps.filter { it.package_name !in hiddenApps } }
    var notes by remember { mutableStateOf(repository.getNotes()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
    var isDarkModeState by remember { mutableStateOf(prefManager.isDarkMode) }

    LaunchedEffect(schema.theme.is_dark) {
        if (schema.theme.is_dark != isDarkModeState) {
            isDarkModeState = schema.theme.is_dark
            prefManager.isDarkMode = schema.theme.is_dark
            isDarkMode = schema.theme.is_dark
        }
    }

    val configManager = remember { LauncherConfigManager.getInstance(context) }
    var configState by remember { mutableStateOf(configManager.config) }

    val notificationBridge = remember { NotificationBridge.getInstance(context) }
    val notifications by notificationBridge.notificationsFlow.collectAsState()
    val marketplaceCatalog = remember { repository.marketplaceCatalog }
    var metrics by remember { mutableStateOf(SystemMetrics()) }

    // ── Overlay / Sheet States ────────────────────────────────────────────────
    var showDrawer by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showMarketplace by remember { mutableStateOf(false) }
    var showMetrics by remember { mutableStateOf(false) }
    var showSchemaEditor by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    var longPressedApp by remember { mutableStateOf<SystemApp?>(null) }
    var longPressedElement by remember { mutableStateOf<LauncherElement?>(null) }
    var previewOriginalTheme by remember { mutableStateOf<LauncherTheme?>(null) }

    // Reset Callback for HOME key press
    LaunchedEffect(Unit) {
        onRegisterResetCallback?.invoke {
            showDrawer = false
            showSearch = false
            showMarketplace = false
            showMetrics = false
            showSchemaEditor = false
            showWebView = false
            showQuickSettings = false
            longPressedApp = null
            longPressedElement = null
        }
    }

    // Back Button Handler: Close open sheets or do nothing (stay on home launcher)
    val isAnyOverlayOpen = showDrawer || showSearch || showMarketplace || showMetrics || showSchemaEditor || showWebView || showQuickSettings || (longPressedApp != null) || (longPressedElement != null)
    BackHandler(enabled = isAnyOverlayOpen) {
        if (longPressedApp != null) {
            longPressedApp = null
        } else if (longPressedElement != null) {
            longPressedElement = null
        } else {
            showDrawer = false
            showSearch = false
            showMarketplace = false
            showMetrics = false
            showSchemaEditor = false
            showWebView = false
            showQuickSettings = false
        }
    }

    // Launch App helper with LatencyManager ultra-low latency bridge & fallback intent handling
    val handleLaunchApp: (SystemApp) -> Unit = { app ->
        repository.recordAppLaunch(app.package_name)
        apps = repository.getApps()

        when (app.package_name) {
            "com.piuu.brain" -> {
                toggleFloatingOverlay(context)
            }
            "com.piuu.ide" -> {
                showSchemaEditor = true
            }
            "com.piuu.suite" -> {
                showMarketplace = true
            }
            "com.piuu.notes" -> {
                toggleFloatingOverlay(context)
            }
            else -> {
                val launched = latencyManager.launchAppFast(context, app.package_name) {
                    Toast.makeText(context, "${app.name} is not installed on this device", Toast.LENGTH_SHORT).show()
                }
                if (!launched) {
                    Log.w("MainActivity", "Could not launch package: ${app.package_name}")
                }
            }
        }
    }

    val handleAutoCategorizeApps: (() -> Unit) -> Unit = { onFinished ->
        coroutineScope.launch {
            try {
                val updatedApps = repository.autoCategorizeInstalledApps(aiEngine)
                apps = updatedApps
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onFinished()
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
                onOpenBrainChat = { toggleFloatingOverlay(context) },
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
                installedApps = visibleApps,
                notes = notes,
                onLaunchApp = handleLaunchApp,
                onOpenDrawer = { showDrawer = true },
                onOpenSearch = { showSearch = true },
                onOpenBrainChat = { toggleFloatingOverlay(context) },
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
                },
                onLongPressApp = { longPressedApp = it },
                onLongPressElement = { longPressedElement = it },
                onSaveSchema = { updatedSchema ->
                    schema = updatedSchema
                    repository.saveSchema(updatedSchema)
                }
            )
        }

        // ── Overlays & Bottom Sheets ─────────────────────────────────────────

        // App Drawer
        AppDrawer(
            visible = showDrawer,
            apps = visibleApps,
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
            },
            onLongPressApp = { app ->
                showDrawer = false
                longPressedApp = app
            },
            onAutoCategorize = handleAutoCategorizeApps
        )

        // Universal Search
        GlobalSearchModal(
            visible = showSearch,
            apps = apps,
            onDismiss = { showSearch = false },
            onLaunchApp = { app ->
                showSearch = false
                handleLaunchApp(app)
            },
            onSystemAction = { actionTarget ->
                when (actionTarget) {
                    "toggle_wifi" -> metrics = metrics.copy(wifi_enabled = !metrics.wifi_enabled)
                    "open_marketplace" -> showMarketplace = true
                    "open_metrics" -> showMetrics = true
                    "open_schema" -> showSchemaEditor = true
                }
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
                        prefManager.iconPackPackageName = packName
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

                val (success, msg) = repository.installMarketplacePlugin(item)
                if (success) {
                    schema = updatedSchema
                    repository.saveSchema(updatedSchema)
                    previewOriginalTheme = null
                    Toast.makeText(context, "Applied '${item.name}' successfully!", Toast.LENGTH_SHORT).show()
                    showMarketplace = false
                } else {
                    Toast.makeText(context, "Not able to apply '${item.name}': $msg", Toast.LENGTH_LONG).show()
                }
            },
            onPreviewTheme = { item ->
                if (previewOriginalTheme == null) {
                    previewOriginalTheme = schema.theme
                }

                val currentTheme = schema.theme
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

                val previewTheme = currentTheme.copy(
                    id = item.id,
                    name = item.name,
                    primary_color = primary,
                    accent_color = accent,
                    bg_overlay = bg,
                    font_family = font
                )
                schema = schema.copy(theme = previewTheme)
                Toast.makeText(context, context.getString(R.string.toast_preview_applied), Toast.LENGTH_SHORT).show()
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

        // Native Control Dashboard Sheet
        if (showWebView) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showWebView = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                NativeControlDashboard(
                    repository = repository,
                    latencyManager = latencyManager,
                    aiEngine = aiEngine,
                    schema = schema,
                    onSaveSchema = { updatedSchema ->
                        schema = updatedSchema
                        repository.saveSchema(updatedSchema)
                    },
                    onDismiss = { showWebView = false },
                    onOpenMarketplace = { showMarketplace = true },
                    onOpenSchemaEditor = { showSchemaEditor = true }
                )
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
                    context.startActivity(intent)
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
            },
            isDarkMode = isDarkModeState,
            onToggleDarkMode = { newIsDark ->
                isDarkModeState = newIsDark
                prefManager.isDarkMode = newIsDark
                isDarkMode = newIsDark
                val updatedTheme = schema.theme.copy(is_dark = newIsDark)
                schema = schema.copy(theme = updatedTheme)
                repository.saveSchema(schema)
                Toast.makeText(context, if (newIsDark) "Switched to Dark Mode" else "Switched to Light Mode", Toast.LENGTH_SHORT).show()
            },
            onAutoCategorize = handleAutoCategorizeApps
        )

        // Context Menu for Long-Press Interactions
        AppContextMenuModal(
            visible = longPressedApp != null,
            app = longPressedApp ?: SystemApp("", "", "", "", "", 0, 0, false, "", ""),
            onDismiss = { longPressedApp = null },
            onUninstallApp = { appToUninstall ->
                repository.deleteApp(appToUninstall.package_name)
                apps = repository.getApps()
                Toast.makeText(context, "'${appToUninstall.name}' uninstalled successfully!", Toast.LENGTH_SHORT).show()
            },
            onAddApp = { appToAdd ->
                repository.addApp(appToAdd)
                apps = repository.getApps()
            },
            onConfigChanged = { updatedConfig ->
                configManager.config = updatedConfig
                configState = updatedConfig
            },
            onPrefChanged = {
                apps = repository.getApps()
                hiddenApps = repository.getHiddenApps()
            },
            allApps = apps
        )

        if (previewOriginalTheme != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardGlassBg)
                    .border(1.5.dp, PrimaryBlue, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.preview_active_banner, schema.theme.name),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "This theme is applied temporarily.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Revert Button
                        OutlinedButton(
                            onClick = {
                                schema = schema.copy(theme = previewOriginalTheme!!)
                                previewOriginalTheme = null
                                Toast.makeText(context, context.getString(R.string.toast_preview_reverted), Toast.LENGTH_SHORT).show()
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextMuted),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(id = R.string.btn_revert_theme), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Keep/Apply Button
                        Button(
                            onClick = {
                                repository.saveSchema(schema)
                                previewOriginalTheme = null
                                Toast.makeText(context, context.getString(R.string.toast_preview_saved), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(id = R.string.btn_keep_theme), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Widget Customizer Dialog Overlay
        if (longPressedElement != null) {
            WidgetCustomizerModal(
                element = longPressedElement,
                onDismiss = { longPressedElement = null },
                onSaveElement = { updatedElem ->
                    val updatedPages = schema.pages.map { page ->
                        val updatedElements = page.elements.map { elem ->
                            if (elem.element_id == updatedElem.element_id) {
                                updatedElem
                            } else {
                                elem
                            }
                        }
                        page.copy(elements = updatedElements)
                    }
                    val updatedSchema = schema.copy(pages = updatedPages)
                    schema = updatedSchema
                    repository.saveSchema(updatedSchema)
                    longPressedElement = null
                    Toast.makeText(context, "Widget customized!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
}

fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    @Suppress("DEPRECATION")
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

fun toggleFloatingOverlay(context: Context) {
    if (android.provider.Settings.canDrawOverlays(context)) {
        if (isServiceRunning(context, FloatingOverlayService::class.java)) {
            FloatingOverlayService.stop(context)
            Toast.makeText(context, "Seamless PIP Floating Overlay Stopped", Toast.LENGTH_SHORT).show()
        } else {
            FloatingOverlayService.start(context)
            Toast.makeText(context, "Seamless PIP Floating Overlay Started", Toast.LENGTH_SHORT).show()
        }
    } else {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        )
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Please grant 'Draw over other apps' permission to start PIP", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            val genericIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            context.startActivity(genericIntent)
            Toast.makeText(context, "Please grant 'Draw over other apps' permission to start PIP", Toast.LENGTH_LONG).show()
        }
    }
}
