package com.piuu.launcher.repository

import android.content.Context
import android.content.SharedPreferences

class LauncherPreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("piuu_launcher_prefs", Context.MODE_PRIVATE)

    var iconPackPackageName: String
        get() = prefs.getString(KEY_ICON_PACK_PACKAGE_NAME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_ICON_PACK_PACKAGE_NAME, value).apply()

    // Gestures Configuration Settings
    var gestureSwipeUp: String
        get() = prefs.getString(KEY_GESTURE_SWIPE_UP, "DRAWER") ?: "DRAWER"
        set(value) = prefs.edit().putString(KEY_GESTURE_SWIPE_UP, value).apply()

    var gestureSwipeDown: String
        get() = prefs.getString(KEY_GESTURE_SWIPE_DOWN, "QUICK_SETTINGS") ?: "QUICK_SETTINGS"
        set(value) = prefs.edit().putString(KEY_GESTURE_SWIPE_DOWN, value).apply()

    var gestureDoubleTap: String
        get() = prefs.getString(KEY_GESTURE_DOUBLE_TAP, "SEARCH") ?: "SEARCH"
        set(value) = prefs.edit().putString(KEY_GESTURE_DOUBLE_TAP, value).apply()

    var gestureSwipeLeft: String
        get() = prefs.getString(KEY_GESTURE_SWIPE_LEFT, "BRAIN_CHAT") ?: "BRAIN_CHAT"
        set(value) = prefs.edit().putString(KEY_GESTURE_SWIPE_LEFT, value).apply()

    var gestureSwipeRight: String
        get() = prefs.getString(KEY_GESTURE_SWIPE_RIGHT, "WEB_VIEW") ?: "WEB_VIEW"
        set(value) = prefs.edit().putString(KEY_GESTURE_SWIPE_RIGHT, value).apply()

    var gestureEdgeSwipe: String
        get() = prefs.getString(KEY_GESTURE_EDGE_SWIPE, "NOTIFICATION_CENTER") ?: "NOTIFICATION_CENTER"
        set(value) = prefs.edit().putString(KEY_GESTURE_EDGE_SWIPE, value).apply()

    var drawerColumnCount: Int
        get() = prefs.getInt(KEY_DRAWER_COLUMN_COUNT, 4)
        set(value) = prefs.edit().putInt(KEY_DRAWER_COLUMN_COUNT, value).apply()

    var drawerScrollMode: String
        get() = prefs.getString(KEY_DRAWER_SCROLL_MODE, "VERTICAL") ?: "VERTICAL"
        set(value) = prefs.edit().putString(KEY_DRAWER_SCROLL_MODE, value).apply()

    var dockIconCount: Int
        get() = prefs.getInt(KEY_DOCK_ICON_COUNT, 5)
        set(value) = prefs.edit().putInt(KEY_DOCK_ICON_COUNT, value).apply()

    var dockBackgroundStyle: String
        get() = prefs.getString(KEY_DOCK_BG_STYLE, "GLASS") ?: "GLASS"
        set(value) = prefs.edit().putString(KEY_DOCK_BG_STYLE, value).apply()

    var dockShowLabels: Boolean
        get() = prefs.getBoolean(KEY_DOCK_SHOW_LABELS, false)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_SHOW_LABELS, value).apply()

    var dockVisible: Boolean
        get() = prefs.getBoolean(KEY_DOCK_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_VISIBLE, value).apply()

    var drawerShowFrequentlyUsed: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_SHOW_FREQUENTLY_USED, true)
        set(value) = prefs.edit().putBoolean(KEY_DRAWER_SHOW_FREQUENTLY_USED, value).apply()

    var drawerShowCategories: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_SHOW_CATEGORIES, true)
        set(value) = prefs.edit().putBoolean(KEY_DRAWER_SHOW_CATEGORIES, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    var wallpaperTransparency: Float
        get() = prefs.getFloat(KEY_WALLPAPER_TRANSPARENCY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_WALLPAPER_TRANSPARENCY, value).apply()

    var appDrawerBlur: Int
        get() = prefs.getInt(KEY_APP_DRAWER_BLUR, 15)
        set(value) = prefs.edit().putInt(KEY_APP_DRAWER_BLUR, value).apply()

    var appDrawerTransparency: Float
        get() = prefs.getFloat(KEY_APP_DRAWER_TRANSPARENCY, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_APP_DRAWER_TRANSPARENCY, value).apply()

    var drawerBackgroundColor: String
        get() = prefs.getString(KEY_DRAWER_BG_COLOR, "#020817") ?: "#020817"
        set(value) = prefs.edit().putString(KEY_DRAWER_BG_COLOR, value).apply()

    var drawerShowLabels: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_SHOW_LABELS, false)
        set(value) = prefs.edit().putBoolean(KEY_DRAWER_SHOW_LABELS, value).apply()

    var drawerHandleVisible: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_HANDLE_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_DRAWER_HANDLE_VISIBLE, value).apply()

    var dockIconSize: Int
        get() = prefs.getInt(KEY_DOCK_ICON_SIZE, 52)
        set(value) = prefs.edit().putInt(KEY_DOCK_ICON_SIZE, value).apply()

    companion object {
        private const val KEY_ICON_PACK_PACKAGE_NAME = "key_icon_pack_package_name"
        private const val KEY_GESTURE_SWIPE_UP = "key_gesture_swipe_up"
        private const val KEY_GESTURE_SWIPE_DOWN = "key_gesture_swipe_down"
        private const val KEY_GESTURE_DOUBLE_TAP = "key_gesture_double_tap"
        private const val KEY_GESTURE_SWIPE_LEFT = "key_gesture_swipe_left"
        private const val KEY_GESTURE_SWIPE_RIGHT = "key_gesture_swipe_right"
        private const val KEY_GESTURE_EDGE_SWIPE = "key_gesture_edge_swipe"
        private const val KEY_DRAWER_COLUMN_COUNT = "key_drawer_column_count"
        private const val KEY_DRAWER_SCROLL_MODE = "key_drawer_scroll_mode"
        private const val KEY_DOCK_ICON_COUNT = "key_dock_icon_count"
        private const val KEY_DOCK_BG_STYLE = "key_dock_bg_style"
        private const val KEY_DOCK_SHOW_LABELS = "key_dock_show_labels"
        private const val KEY_DOCK_VISIBLE = "key_dock_visible"
        private const val KEY_DRAWER_SHOW_FREQUENTLY_USED = "key_drawer_show_frequently_used"
        private const val KEY_DRAWER_SHOW_CATEGORIES = "key_drawer_show_categories"
        private const val KEY_IS_DARK_MODE = "key_is_dark_mode"
        private const val KEY_WALLPAPER_TRANSPARENCY = "key_wallpaper_transparency"
        private const val KEY_APP_DRAWER_BLUR = "key_app_drawer_blur"
        private const val KEY_APP_DRAWER_TRANSPARENCY = "key_app_drawer_transparency"
        private const val KEY_DRAWER_BG_COLOR = "key_drawer_bg_color"
        private const val KEY_DRAWER_SHOW_LABELS = "key_drawer_show_labels"
        private const val KEY_DRAWER_HANDLE_VISIBLE = "key_drawer_handle_visible"
        private const val KEY_DOCK_ICON_SIZE = "key_dock_icon_size"

        val GESTURE_ACTIONS = listOf(
            "DRAWER" to "Open App Drawer",
            "NOTIFICATION_CENTER" to "Toggle Notification Center",
            "SEARCH" to "Global Search",
            "BRAIN_CHAT" to "AI Brain Chat",
            "QUICK_SETTINGS" to "Quick Settings",
            "WEB_VIEW" to "Hybrid WebView Feed",
            "MARKETPLACE" to "App Marketplace",
            "METRICS" to "System Metrics",
            "SCHEMA_EDITOR" to "Schema Editor",
            "NONE" to "None (Disabled)"
        )

        @Volatile
        private var INSTANCE: LauncherPreferenceManager? = null

        fun getInstance(context: Context): LauncherPreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LauncherPreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
