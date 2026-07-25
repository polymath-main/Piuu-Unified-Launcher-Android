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

    var drawerColumnCount: Int
        get() = prefs.getInt(KEY_DRAWER_COLUMN_COUNT, 4)
        set(value) = prefs.edit().putInt(KEY_DRAWER_COLUMN_COUNT, value).apply()

    var drawerShowFrequentlyUsed: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_SHOW_FREQUENTLY_USED, true)
        set(value) = prefs.edit().putBoolean(KEY_DRAWER_SHOW_FREQUENTLY_USED, value).apply()

    var drawerShowCategories: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_SHOW_CATEGORIES, true)
        set(value) = prefs.edit().putBoolean(KEY_DRAWER_SHOW_CATEGORIES, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    companion object {
        private const val KEY_ICON_PACK_PACKAGE_NAME = "key_icon_pack_package_name"
        private const val KEY_GESTURE_SWIPE_UP = "key_gesture_swipe_up"
        private const val KEY_GESTURE_SWIPE_DOWN = "key_gesture_swipe_down"
        private const val KEY_GESTURE_DOUBLE_TAP = "key_gesture_double_tap"
        private const val KEY_GESTURE_SWIPE_LEFT = "key_gesture_swipe_left"
        private const val KEY_GESTURE_SWIPE_RIGHT = "key_gesture_swipe_right"
        private const val KEY_DRAWER_COLUMN_COUNT = "key_drawer_column_count"
        private const val KEY_DRAWER_SHOW_FREQUENTLY_USED = "key_drawer_show_frequently_used"
        private const val KEY_DRAWER_SHOW_CATEGORIES = "key_drawer_show_categories"
        private const val KEY_IS_DARK_MODE = "key_is_dark_mode"

        val GESTURE_ACTIONS = listOf(
            "DRAWER" to "Open App Drawer",
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
