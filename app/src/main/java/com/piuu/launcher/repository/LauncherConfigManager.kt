package com.piuu.launcher.repository

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebView
import org.json.JSONObject

data class LauncherConfig(
    val homeIconSize: Int = 58,
    val drawerIconSize: Int = 58,
    val drawerOrientation: String = "vertical", // "vertical" | "horizontal"
    val homeColumns: Int = 4,
    val homeRows: Int = 6,
    val drawerColumns: Int = 4,
    val backgroundTransparency: Float = 0.85f, // 0.0 to 1.0
    val showSystemWallpaper: Boolean = true
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
            backgroundTransparency = prefs.getFloat("bg_transparency", 0.85f),
            showSystemWallpaper = prefs.getBoolean("show_system_wallpaper", true)
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
                .putBoolean("show_system_wallpaper", value.showSystemWallpaper)
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
        json.put("showSystemWallpaper", c.showSystemWallpaper)
        return json.toString()
    }

    fun setWallpaperEnabled(enabled: Boolean) {
        val current = config
        config = current.copy(showSystemWallpaper = enabled)
    }

    fun updateConfigFromJson(jsonStr: String, webView: WebView?) {
        try {
            val json = JSONObject(jsonStr)
            val current = config
            val updated = LauncherConfig(
                homeIconSize = json.optInt("homeIconSize", current.homeIconSize),
                drawerIconSize = json.optInt("drawerIconSize", current.drawerIconSize),
                drawerOrientation = json.optString("drawerOrientation", current.drawerOrientation),
                homeColumns = json.optInt("homeColumns", current.homeColumns),
                homeRows = json.optInt("homeRows", current.homeRows),
                drawerColumns = json.optInt("drawerColumns", current.drawerColumns),
                backgroundTransparency = json.optDouble("backgroundTransparency", current.backgroundTransparency.toDouble()).toFloat(),
                showSystemWallpaper = json.optBoolean("showSystemWallpaper", current.showSystemWallpaper)
            )
            config = updated

            // Push configuration update over bridge to WebView in single frame
            webView?.post {
                webView.evaluateJavascript("window.onConfigChanged && window.onConfigChanged('${getConfigJson()}')", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LauncherConfigManager? = null

        fun getInstance(context: Context): LauncherConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LauncherConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
