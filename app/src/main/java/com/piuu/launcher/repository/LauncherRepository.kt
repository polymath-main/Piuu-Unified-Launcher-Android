package com.piuu.launcher.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.piuu.launcher.model.*
import java.text.SimpleDateFormat
import java.util.*

class LauncherRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("piuu_launcher_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

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

    // ── Default Marketplace Catalog loaded from SchemeStore ─────────────────────
    val marketplaceCatalog: List<MarketplaceItem>
        get() {
            val store = SchemeManager.getInstance(context)
            val categories = listOf("themes", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills")
            val items = mutableListOf<MarketplaceItem>()
            for (category in categories) {
                val schemes = store.getSchemesForCategory(category)
                for (scheme in schemes) {
                    val cat = when (category) {
                        "themes" -> "theming"
                        else -> category
                    }
                    items.add(
                        MarketplaceItem(
                            id = scheme.id,
                            name = scheme.name,
                            category = cat,
                            author = scheme.author,
                            rating = 4.8f,
                            downloads = 1200,
                            description = scheme.description,
                            preview_badge = if (scheme.isCustom) "Custom" else "Official",
                            payload = scheme.payload,
                            is_installed = true
                        )
                    )
                }
            }
            return items
        }

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
        if (json != null) {
            try {
                return gson.fromJson(json, LauncherSchema::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val defaultSchema = createDefaultSchema()
        saveSchema(defaultSchema)
        return defaultSchema
    }

    fun saveSchema(schema: LauncherSchema) {
        prefs.edit().putString("launcher_schema", gson.toJson(schema)).apply()
    }

    fun resetSchema(): LauncherSchema {
        val schema = createDefaultSchema()
        saveSchema(schema)
        return schema
    }

    // ── Get / Save Apps ─────────────────────────────────────────────────────────
    fun getApps(): List<SystemApp> {
        val json = prefs.getString("installed_apps", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<SystemApp>>() {}.type
                return gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        saveApps(defaultApps)
        return defaultApps
    }

    fun saveApps(apps: List<SystemApp>) {
        prefs.edit().putString("installed_apps", gson.toJson(apps)).apply()
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
