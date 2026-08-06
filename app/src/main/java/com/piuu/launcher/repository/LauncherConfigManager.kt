package com.piuu.launcher.repository

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

data class LauncherConfig(
    val homeIconSize: Int = 58,
    val drawerIconSize: Int = 58,
    val drawerOrientation: String = "vertical", // "vertical" | "horizontal"
    val homeColumns: Int = 4,
    val homeRows: Int = 6,
    val drawerColumns: Int = 4,
    val backgroundTransparency: Float = 0.0f, // 0.0f = Raw Wallpaper View
    val enableWallpaperMask: Boolean = false,
    val showSystemWallpaper: Boolean = true,
    val pipEdgeSide: String = "right", // "left" | "right"
    val pipBarWidth: Int = 12,
    val pipBarHeight: Int = 72,
    val pipIconSize: Int = 24
)

class LauncherConfigManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("piuu_launcher_config", Context.MODE_PRIVATE)

    var config: LauncherConfig
        get() = LauncherConfig(
            homeIconSize = prefs.getInt("home_icon_size", 58),
            drawerIconSize = prefs.getInt("drawer_icon_size", 58),
            drawerOrientation = prefs.getString("drawer_orientation", "vertical") ?: "vertical",
            homeColumns = prefs.getInt("home_columns", 4),
            homeRows = prefs.getInt("home_rows", 6),
            drawerColumns = prefs.getInt("drawer_columns", 4),
            backgroundTransparency = prefs.getFloat("bg_transparency", 0.0f),
            enableWallpaperMask = prefs.getBoolean("enable_wallpaper_mask", false),
            showSystemWallpaper = prefs.getBoolean("show_system_wallpaper", true),
            pipEdgeSide = prefs.getString("pip_edge_side", "right") ?: "right",
            pipBarWidth = prefs.getInt("pip_bar_width", 12),
            pipBarHeight = prefs.getInt("pip_bar_height", 72),
            pipIconSize = prefs.getInt("pip_icon_size", 24)
        )
        set(value) {
            prefs.edit()
                .putInt("home_icon_size", value.homeIconSize)
                .putInt("drawer_icon_size", value.drawerIconSize)
                .putString("drawer_orientation", value.drawerOrientation)
                .putInt("home_columns", value.homeColumns)
                .putInt("home_rows", value.homeRows)
                .putInt("drawer_columns", value.drawerColumns)
                .putFloat("bg_transparency", value.backgroundTransparency)
                .putBoolean("enable_wallpaper_mask", value.enableWallpaperMask)
                .putBoolean("show_system_wallpaper", value.showSystemWallpaper)
                .putString("pip_edge_side", value.pipEdgeSide)
                .putInt("pip_bar_width", value.pipBarWidth)
                .putInt("pip_bar_height", value.pipBarHeight)
                .putInt("pip_icon_size", value.pipIconSize)
                .apply()
        }

    fun getConfigJson(): String {
        val c = config
        val json = JSONObject()
        json.put("homeIconSize", c.homeIconSize)
        json.put("drawerIconSize", c.drawerIconSize)
        json.put("drawerOrientation", c.drawerOrientation)
        json.put("homeColumns", c.homeColumns)
        json.put("homeRows", c.homeRows)
        json.put("drawerColumns", c.drawerColumns)
        json.put("backgroundTransparency", c.backgroundTransparency.toDouble())
        json.put("enableWallpaperMask", c.enableWallpaperMask)
        json.put("showSystemWallpaper", c.showSystemWallpaper)
        json.put("pipEdgeSide", c.pipEdgeSide)
        json.put("pipBarWidth", c.pipBarWidth)
        json.put("pipBarHeight", c.pipBarHeight)
        json.put("pipIconSize", c.pipIconSize)
        return json.toString()
    }

    fun setWallpaperEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("show_system_wallpaper", enabled).apply()
    }

    companion object {
        @Volatile
        private var instance: LauncherConfigManager? = null

        fun getInstance(context: Context): LauncherConfigManager {
            return instance ?: synchronized(this) {
                instance ?: LauncherConfigManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
