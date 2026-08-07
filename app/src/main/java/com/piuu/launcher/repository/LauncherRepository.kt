package com.piuu.launcher.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.piuu.launcher.database.AppUsageEntity
import com.piuu.launcher.database.DockShortcutEntity
import com.piuu.launcher.database.HomeWidgetEntity
import com.piuu.launcher.database.PiuuLauncherDatabase
import com.piuu.launcher.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LauncherRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("piuu_launcher_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val db = PiuuLauncherDatabase.getInstance(context)
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // ── Default Apps Database ───────────────────────────────────────────────────
    val defaultApps = listOf(
        SystemApp("com.android.phone", "Phone", "phone", "system", "intent://phone", 142, 0, true, "#10B981", "Make calls & manage contacts"),
        SystemApp("com.android.mms", "Messages", "message", "social", "intent://messages", 118, 3, true, "#3B82F6", "SMS & Chat messaging"),
        SystemApp("com.android.chrome", "Chrome", "globe", "utilities", "intent://chrome", 210, 0, true, "#EF4444", "Fast web browser"),
        SystemApp("com.android.camera", "Camera", "camera", "creative", "intent://camera", 88, 0, true, "#8B5CF6", "Capture photos & video"),
        SystemApp("com.piuu.brain", "Piuu Brain", "brain", "piuu_suite", "intent://piuu_brain", 312, 0, true, "#6366F1", "AI Copilot & Assistant"),
        SystemApp("com.piuu.suite", "Piuu Suite", "folder", "piuu_suite", "intent://piuu_suite", 154, 0, false, "#EC4899", "Unified Productivity Hub"),
        SystemApp("com.piuu.ide", "Piuu IDE", "code", "piuu_suite", "intent://piuu_ide", 95, 0, false, "#10B981", "Schema & Code Editor"),
        SystemApp("com.spotify.music", "Spotify", "music", "entertainment", "intent://spotify", 175, 0, false, "#22C55E", "Music & Podcasts"),
        SystemApp("com.google.android.gm", "Gmail", "mail", "productivity", "intent://gmail", 164, 5, false, "#EA4335", "Email inbox"),
        SystemApp("com.google.android.youtube", "YouTube", "video", "entertainment", "intent://youtube", 230, 1, false, "#FF0000", "Videos & Shorts"),
        SystemApp("com.google.android.calendar", "Calendar", "calendar", "productivity", "intent://calendar", 92, 0, false, "#4285F4", "Events & Schedules"),
        SystemApp("com.android.settings", "Settings", "settings", "system", "intent://settings", 64, 0, false, "#64748B", "System preferences"),
        SystemApp("com.piuu.notes", "Notes", "note", "productivity", "intent://notes", 80, 0, false, "#F59E0B", "Quick notepad"),
        SystemApp("com.android.calculator2", "Calculator", "calculator", "utilities", "intent://calculator", 45, 0, false, "#0EA5E9", "Math tool"),
        SystemApp("com.android.weather", "Weather", "cloud", "utilities", "intent://weather", 110, 0, false, "#38BDF8", "Forecast & Radar"),
        SystemApp("com.android.documentsui", "Files", "file", "system", "intent://files", 38, 0, false, "#A855F7", "File browser"),
        SystemApp("com.android.deskclock", "Clock", "clock", "utilities", "intent://clock", 125, 0, false, "#F43F5E", "Alarms & Stopwatch"),
        SystemApp("com.instagram.android", "Instagram", "camera", "social", "intent://instagram", 190, 12, false, "#E1306C", "Photos & Stories"),
        SystemApp("com.twitter.android", "X / Twitter", "share", "social", "intent://twitter", 130, 8, false, "#1DA1F2", "Live social feed"),
        SystemApp("com.whatsapp", "WhatsApp", "message", "social", "intent://whatsapp", 340, 14, false, "#25D366", "Instant messaging")
    )

    // ── Marketplace Catalog loaded from MarketplaceCore Engine ────────────────
    private val marketplaceCore = com.piuu.launcher.marketplace.MarketplaceCore.getInstance(context)

    fun getMarketplaceCatalog(currentSchema: LauncherSchema? = null): List<MarketplaceItem> {
        val schema = currentSchema ?: getSchema()
        return marketplaceCore.getFullCatalog(schema)
    }

    fun installMarketplacePlugin(item: MarketplaceItem): Pair<Boolean, String> {
        return marketplaceCore.installPlugin(item)
    }

    fun uninstallMarketplacePlugin(pluginId: String): Pair<Boolean, String> {
        return marketplaceCore.uninstallPlugin(pluginId)
    }

    fun toggleMarketplacePluginEnabled(pluginId: String, enable: Boolean): Boolean {
        return marketplaceCore.togglePluginEnabled(pluginId, enable)
    }

    fun registerCustomPlugin(manifestJson: String, payloadJson: String): Pair<Boolean, String> {
        return marketplaceCore.registerCustomPluginFromManifest(manifestJson, payloadJson)
    }

    fun executePluginSdkAction(pluginId: String, actionName: String): com.piuu.launcher.marketplace.PluginExecutionResult {
        return marketplaceCore.executePluginSdkAction(pluginId, actionName)
    }

    val marketplaceCatalog: List<MarketplaceItem>
        get() = getMarketplaceCatalog()

    // ── Initial Notifications ───────────────────────────────────────────────────
    val defaultNotifications = mutableListOf(
        NotificationItem("n1", "Piuu Brain", "Morning Digest Ready", "Weather is 24°C Sunny. You have 2 calendar events today.", "09:15 AM", "brain"),
        NotificationItem("n2", "Messages", "Alex", "Are we still meeting at 3 PM for the launcher review?", "08:42 AM", "message"),
        NotificationItem("n3", "System Health", "Optimization Complete", "RAM cleared (1.2 GB freed). Battery is at 86%.", "07:30 AM", "settings")
    )

    // ── Default Notes ───────────────────────────────────────────────────────────
    val defaultNotes = mutableListOf(
        NoteItem("note1", "1. Review Piuu Launcher Kotlin architecture", "Today 10:00 AM", "#3B82F6"),
        NoteItem("note2", "2. Explore Marketplace themes & widgets", "Today 11:30 AM", "#8B5CF6")
    )

    // ── Get / Save Schema ───────────────────────────────────────────────────────
    fun getSchema(): LauncherSchema {
        val json = prefs.getString("launcher_schema", null)
        var schema = if (json != null) {
            try {
                gson.fromJson(json, LauncherSchema::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                val defaultSchema = createDefaultSchema()
                saveSchema(defaultSchema)
                defaultSchema
            }
        } else {
            val defaultSchema = createDefaultSchema()
            saveSchema(defaultSchema)
            defaultSchema
        }

        // Synchronize Home Screen page widgets/icons grid layouts and dock shortcuts from SQLite Room database
        try {
            val widgetList = kotlinx.coroutines.runBlocking {
                db.launcherDao().getAllWidgetsSync()
            }
            val dockShortcuts = kotlinx.coroutines.runBlocking {
                db.launcherDao().getDockShortcutsSync()
            }

            if (widgetList.isNotEmpty() || dockShortcuts.isNotEmpty()) {
                // Reconstruct pages based on SQLite Room DB positions
                val pages = schema.pages.mapIndexed { pageIdx, page ->
                    val pageWidgets = widgetList.filter { it.pageIndex == pageIdx }
                    if (pageWidgets.isNotEmpty()) {
                        val elements = pageWidgets.map { entity ->
                            val styleProps = try {
                                gson.fromJson(entity.propsJson, StyleProps::class.java)
                            } catch (_: Exception) {
                                StyleProps(
                                    x = entity.x,
                                    y = entity.y,
                                    w = entity.w,
                                    h = entity.h
                                )
                            }.copy(
                                x = entity.x,
                                y = entity.y,
                                w = entity.w,
                                h = entity.h
                            )
                            LauncherElement(
                                element_id = entity.elementId,
                                type = ElementType.valueOf(entity.type),
                                title = entity.title,
                                style_props = styleProps
                            )
                        }
                        page.copy(elements = elements)
                    } else {
                        page
                    }
                }

                val dock = if (dockShortcuts.isNotEmpty()) {
                    schema.dock.copy(app_packages = dockShortcuts.map { it.packageName })
                } else {
                    schema.dock
                }

                schema = schema.copy(pages = pages, dock = dock)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return schema
    }

    fun saveSchema(schema: LauncherSchema) {
        prefs.edit().putString("launcher_schema", gson.toJson(schema)).apply()
        
        // Persist Home Widgets and Dock Shortcuts into SQLite Room Database
        repositoryScope.launch {
            try {
                val widgetEntities = mutableListOf<HomeWidgetEntity>()
                schema.pages.forEachIndexed { pageIdx, page ->
                    page.elements.forEach { elem ->
                        widgetEntities.add(
                            HomeWidgetEntity(
                                elementId = elem.element_id,
                                type = elem.type.name,
                                pageIndex = pageIdx,
                                x = elem.style_props.x ?: 0,
                                y = elem.style_props.y ?: 0,
                                w = elem.style_props.w ?: 1,
                                h = elem.style_props.h ?: 1,
                                title = elem.title ?: "",
                                propsJson = gson.toJson(elem.style_props)
                            )
                        )
                    }
                }
                db.launcherDao().clearAllWidgets()
                db.launcherDao().insertAllWidgets(widgetEntities)

                val dockEntities = schema.dock.app_packages.mapIndexed { idx, pkg ->
                    DockShortcutEntity(packageName = pkg, position = idx)
                }
                db.launcherDao().clearDockShortcuts()
                db.launcherDao().setDockShortcuts(dockEntities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addShortcutToHome(packageName: String, appName: String): Boolean {
        try {
            val schema = getSchema()
            if (schema.pages.isEmpty()) return false
            val firstPage = schema.pages.first()
            val elements = firstPage.elements.toMutableList()

            // Find a vacant spot in a 4x6 grid (cols x rows)
            val cols = schema.pages.firstOrNull()?.elements?.maxOfOrNull { it.style_props.x ?: 0 }?.plus(2)?.coerceIn(4, 5) ?: 4
            val rows = 6
            var vacantX = -1
            var vacantY = -1

            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    // Check overlap
                    val hasOverlap = elements.any { elem ->
                        val ex = elem.style_props.x ?: 0
                        val ey = elem.style_props.y ?: 0
                        val ew = elem.style_props.w ?: 1
                        val eh = elem.style_props.h ?: 1
                        x >= ex && x < ex + ew && y >= ey && y < ey + eh
                    }
                    if (!hasOverlap) {
                        vacantX = x
                        vacantY = y
                        break
                    }
                }
                if (vacantX != -1) break
            }

            if (vacantX == -1) {
                val maxY = elements.maxOfOrNull { (it.style_props.y ?: 0) + (it.style_props.h ?: 1) } ?: 0
                vacantX = 0
                vacantY = maxY
            }

            val newElement = LauncherElement(
                element_id = "elem_${System.currentTimeMillis()}",
                type = ElementType.APP_GRID,
                title = appName,
                style_props = StyleProps(x = vacantX, y = vacantY, w = 1, h = 1),
                action_intent = ActionIntent(type = "launch_app", target = packageName)
            )

            elements.add(newElement)

            val updatedPages = schema.pages.toMutableList().apply {
                this[0] = firstPage.copy(elements = elements)
            }

            saveSchema(schema.copy(pages = updatedPages))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun togglePinDock(packageName: String): Boolean {
        try {
            val schema = getSchema()
            val dockApps = schema.dock.app_packages.toMutableList()
            if (dockApps.contains(packageName)) {
                dockApps.remove(packageName)
            } else {
                dockApps.add(packageName)
            }
            saveSchema(schema.copy(dock = schema.dock.copy(app_packages = dockApps)))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun resetSchema(): LauncherSchema {
        val schema = createDefaultSchema()
        saveSchema(schema)
        return schema
    }

    // ── Get / Save Apps ─────────────────────────────────────────────────────────
    fun getApps(): List<SystemApp> {
        val json = prefs.getString("installed_apps", null)
        var list = if (json != null) {
            try {
                val type = object : TypeToken<List<SystemApp>>() {}.type
                gson.fromJson<List<SystemApp>>(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            // Scan and import real device applications on first-run startup
            val scanned = scanAndSaveInstalledApps(context)
            if (scanned.isNotEmpty()) {
                scanned
            } else {
                saveApps(defaultApps)
                defaultApps
            }
        }

        // Synchronize with SQLite Room Database for app usage frequency and favorites
        try {
            val usageList = kotlinx.coroutines.runBlocking {
                db.launcherDao().getAllAppUsageSync()
            }
            if (usageList.isNotEmpty()) {
                val usageMap = usageList.associateBy { it.packageName }
                list = list.map { app ->
                    val usage = usageMap[app.package_name]
                    if (usage != null) {
                        app.copy(
                            usage_count = usage.usageCount,
                            is_favorite = usage.isFavorite
                        )
                    } else {
                        app
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    fun scanAndSaveInstalledApps(ctx: Context): List<SystemApp> {
        try {
            val pm = ctx.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolvedApps = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val currentSavedApps = try {
                val json = prefs.getString("installed_apps", null)
                if (json != null) {
                    val type = object : TypeToken<List<SystemApp>>() {}.type
                    gson.fromJson<List<SystemApp>>(json, type).associateBy { it.package_name }
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                emptyMap()
            }

            val updatedAppsList = mutableListOf<SystemApp>()

            for (resolveInfo in resolvedApps) {
                val pkgName = resolveInfo.activityInfo.packageName
                // Skip this launcher application from appearing in the user's app drawer
                if (pkgName == ctx.packageName) continue

                val label = resolveInfo.loadLabel(pm).toString()

                val existing = currentSavedApps[pkgName]
                if (existing != null) {
                    updatedAppsList.add(existing.copy(name = label))
                } else {
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val category = categorizePackage(pkgName, appInfo)
                    val colorHex = stringToHexColor(pkgName)
                    updatedAppsList.add(
                        SystemApp(
                            package_name = pkgName,
                            name = label,
                            icon_name = "apps",
                            category = category,
                            launch_intent = "intent://$pkgName",
                            usage_count = 0,
                            badge_count = 0,
                            is_favorite = false,
                            accent_color = colorHex,
                            description = "Installed Application ($pkgName)"
                        )
                    )
                }
            }

            // Retain Piuu suite built-in entries
            for (defaultApp in defaultApps) {
                if (defaultApp.package_name.startsWith("com.piuu.") && updatedAppsList.none { it.package_name == defaultApp.package_name }) {
                    updatedAppsList.add(defaultApp)
                }
            }

            if (updatedAppsList.isNotEmpty()) {
                saveApps(updatedAppsList)
                return updatedAppsList
            }
        } catch (e: Exception) {
            Log.e("LauncherRepository", "Failed to scan device packages", e)
        }
        return emptyList()
    }

    private fun categorizePackage(pkgName: String, appInfo: ApplicationInfo?): String {
        if (pkgName.startsWith("com.piuu.")) return "piuu_suite"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_NEWS -> return "social"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "productivity"
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_GAME -> return "entertainment"
                ApplicationInfo.CATEGORY_IMAGE -> return "creative"
                ApplicationInfo.CATEGORY_MAPS -> return "utilities"
            }
        }

        val lower = pkgName.lowercase()
        return when {
            lower.contains("social") || lower.contains("instagram") || lower.contains("twitter") ||
            lower.contains("facebook") || lower.contains("whatsapp") || lower.contains("telegram") ||
            lower.contains("messenger") || lower.contains("snapchat") || lower.contains("tiktok") ||
            lower.contains("linkedin") || lower.contains("discord") || lower.contains("reddit") -> "social"

            lower.contains("mail") || lower.contains("gmail") || lower.contains("calendar") ||
            lower.contains("note") || lower.contains("doc") || lower.contains("sheet") ||
            lower.contains("task") || lower.contains("keep") || lower.contains("office") ||
            lower.contains("todo") -> "productivity"

            lower.contains("youtube") || lower.contains("spotify") || lower.contains("music") ||
            lower.contains("video") || lower.contains("netflix") || lower.contains("game") ||
            lower.contains("stream") || lower.contains("media") || lower.contains("player") -> "entertainment"

            lower.contains("chrome") || lower.contains("browser") || lower.contains("calc") ||
            lower.contains("weather") || lower.contains("clock") || lower.contains("tool") ||
            lower.contains("utility") || lower.contains("map") -> "utilities"

            lower.contains("camera") || lower.contains("photo") || lower.contains("design") ||
            lower.contains("edit") || lower.contains("canvas") || lower.contains("paint") -> "creative"

            (appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0 -> "system"

            else -> "utilities"
        }
    }

    private fun stringToHexColor(str: String): String {
        val hash = str.hashCode()
        val r = (hash and 0xFF0000) shr 16
        val g = (hash and 0x00FF00) shr 8
        val b = hash and 0x0000FF
        return String.format("#%02X%02X%02X", (r + 100) % 256, (g + 100) % 256, (b + 100) % 256)
    }

    suspend fun autoCategorizeInstalledApps(aiEngine: AiEngine): List<SystemApp> {
        val currentApps = getApps()
        val categorizedPairs = aiEngine.categorizeApps(currentApps)
        val categoryMap = categorizedPairs.toMap()

        val updatedApps = currentApps.map { app ->
            val newCat = categoryMap[app.package_name]
            if (newCat != null) {
                app.copy(category = newCat)
            } else {
                app
            }
        }
        saveApps(updatedApps)
        return updatedApps
    }

    fun saveApps(apps: List<SystemApp>) {
        prefs.edit().putString("installed_apps", gson.toJson(apps)).apply()

        // Sync app usage stats and favorite flags into SQLite Room Database
        repositoryScope.launch {
            try {
                val usageEntities = apps.map {
                    AppUsageEntity(
                        packageName = it.package_name,
                        appName = it.name,
                        usageCount = it.usage_count,
                        isFavorite = it.is_favorite
                    )
                }
                db.launcherDao().insertAllAppUsage(usageEntities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reorderApps(packageNames: List<String>): Boolean {
        val currentApps = getApps()
        val reordered = mutableListOf<SystemApp>()
        for (pkg in packageNames) {
            val app = currentApps.find { it.package_name == pkg }
            if (app != null) {
                reordered.add(app)
            }
        }
        for (app in currentApps) {
            if (!reordered.any { it.package_name == app.package_name }) {
                reordered.add(app)
            }
        }
        saveApps(reordered)
        return true
    }

    fun toggleAppFavorite(packageName: String): Boolean {
        val apps = getApps().toMutableList()
        val index = apps.indexOfFirst { it.package_name == packageName }
        if (index != -1) {
            val app = apps[index]
            val newValue = !app.is_favorite
            val updatedApp = app.copy(is_favorite = newValue)
            apps[index] = updatedApp
            saveApps(apps)
            return newValue
        }
        return false
    }

    fun recordAppLaunch(packageName: String) {
        val apps = getApps().toMutableList()
        val index = apps.indexOfFirst { it.package_name == packageName }
        if (index != -1) {
            val app = apps[index]
            app.usage_count += 1
            apps[index] = app
            saveApps(apps)
        }
    }

    fun addApp(app: SystemApp): Boolean {
        val apps = getApps().toMutableList()
        if (apps.any { it.package_name == app.package_name }) {
            return false
        }
        apps.add(app)
        saveApps(apps)
        return true
    }

    fun deleteApp(packageName: String): Boolean {
        val apps = getApps().toMutableList()
        val index = apps.indexOfFirst { it.package_name == packageName }
        if (index != -1) {
            apps.removeAt(index)
            saveApps(apps)
            return true
        }
        return false
    }

    fun updateAppCategory(packageName: String, newCategory: String): Boolean {
        val apps = getApps().toMutableList()
        val index = apps.indexOfFirst { it.package_name == packageName }
        if (index != -1) {
            apps[index] = apps[index].copy(category = newCategory.lowercase())
            saveApps(apps)
            return true
        }
        return false
    }

    fun updateAppCustomName(packageName: String, customName: String): Boolean {
        val apps = getApps().toMutableList()
        val index = apps.indexOfFirst { it.package_name == packageName }
        if (index != -1) {
            apps[index] = apps[index].copy(name = customName)
            saveApps(apps)
            return true
        }
        return false
    }

    fun updateAppAccentColor(packageName: String, colorHex: String): Boolean {
        val apps = getApps().toMutableList()
        val index = apps.indexOfFirst { it.package_name == packageName }
        if (index != -1) {
            apps[index] = apps[index].copy(accent_color = colorHex)
            saveApps(apps)
            return true
        }
        return false
    }

    // ── Hidden Apps Storage ───────────────────────────────────────────────────
    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()
    }

    fun hideApp(packageName: String) {
        val current = getHiddenApps().toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet("hidden_packages", current).apply()
    }

    fun unhideApp(packageName: String) {
        val current = getHiddenApps().toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet("hidden_packages", current).apply()
    }

    // ── Notes persistence ───────────────────────────────────────────────────────
    fun getNotes(): List<NoteItem> {
        val json = prefs.getString("launcher_notes", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<NoteItem>>() {}.type
                return gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        saveNotes(defaultNotes)
        return defaultNotes
    }

    fun saveNotes(notes: List<NoteItem>) {
        prefs.edit().putString("launcher_notes", gson.toJson(notes)).apply()
    }

    // ── Create Default Schema ────────────────────────────────────────────────────
    private fun createDefaultSchema(): LauncherSchema {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val page1Elements = listOf(
            LauncherElement("elem_clock", ElementType.CLOCK, "Digital Clock", StyleProps(w = 2, h = 2)),
            LauncherElement("elem_weather", ElementType.WEATHER, "Weather Radar", StyleProps(w = 2, h = 1)),
            LauncherElement("elem_battery", ElementType.BATTERY_STATUS, "Battery Monitor", StyleProps(w = 2, h = 1)),
            LauncherElement("elem_ai_insight", ElementType.AGENT_WIDGET, "Piuu Neural Assistant", StyleProps(w = 4, h = 2))
        )

        val page2Elements = listOf(
            LauncherElement("elem_media", ElementType.MEDIA_PLAYER, "Media Player", StyleProps(w = 4, h = 2)),
            LauncherElement("elem_stats", ElementType.SYSTEM_STATS, "System Health Monitor", StyleProps(w = 2, h = 2)),
            LauncherElement("elem_notes", ElementType.QUICK_NOTES, "Quick Notepad", StyleProps(w = 2, h = 2))
        )

        val page3Elements = listOf(
            LauncherElement("elem_suite", ElementType.PIUU_SUITE_FOLDER, "Piuu Workspace Suite", StyleProps(w = 2, h = 2)),
            LauncherElement("elem_predictive", ElementType.PREDICTIVE_SUGGESTIONS, "Smart Suggestions", StyleProps(w = 2, h = 2)),
            LauncherElement("elem_app_grid", ElementType.APP_GRID, "Frequent Apps", StyleProps(w = 4, h = 2))
        )

        val pages = listOf(
            LauncherPage("page_home", "Home", page1Elements),
            LauncherPage("page_widgets", "Dashboard", page2Elements),
            LauncherPage("page_suite", "Apps & Suite", page3Elements)
        )

        val dock = LauncherDock(
            element_id = "dock_main",
            app_packages = listOf(
                "com.android.phone",
                "com.android.mms",
                "com.piuu.brain",
                "com.android.chrome",
                "com.android.camera"
            )
        )

        val theme = LauncherTheme(
            id = "theme_nord",
            name = "Nordic Glass",
            author = "Piuu Studio",
            preview_bg = "#0F172A",
            wallpaper_url = "",
            is_dark = true,
            glass_blur = true,
            primary_color = "#3B82F6",
            accent_color = "#8B5CF6",
            bg_overlay = "rgba(2, 8, 23, 0.85)"
        )

        return LauncherSchema(
            schema_id = "schema_v1_nord",
            version = "1.0.0",
            updated_at = now,
            theme = theme,
            pages = pages,
            dock = dock
        )
    }
}
