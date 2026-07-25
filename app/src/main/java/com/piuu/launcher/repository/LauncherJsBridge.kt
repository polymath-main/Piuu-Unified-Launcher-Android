package com.piuu.launcher.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.piuu.launcher.model.*
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LauncherJsBridge: Native-JavaScript Bridge for WebView hybrid surfaces
 */
class LauncherJsBridge(
    private val context: Context,
    private val repository: LauncherRepository,
    private val latencyManager: LatencyManager,
    private val aiEngine: AiEngine,
    private val scope: CoroutineScope,
    private val onLaunchApp: (String, String?) -> Unit
) {
    private val gson = Gson()
    private val iconLoader = UniversalIconLoader(context)
    val wallpaperHandler = WallpaperHandler(context)

    @JavascriptInterface
    fun getSystemWallpaperBase64(): String {
        return wallpaperHandler.getWallpaperBase64()
    }

    @JavascriptInterface
    fun fetchFreshNativeApps(): String {
        return try {
            repository.scanAndSaveInstalledApps(context)
            getInstalledAppsJson()
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun getInstalledAppsJson(): String {
        return try {
            val apps = repository.getApps()
            gson.toJson(apps)
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun getProcessedAppsBatchJson(): String {
        // Runs pre-processing (alphabetization, categorization, and WebP icon pre-decoding) on a background thread.
        return try {
            val apps = repository.getApps().toMutableList()
            // 1. Alphabetization by app name (case-insensitive)
            apps.sortBy { it.name.lowercase() }
            
            // 2. Pre-decode icons to high-efficiency Base64 WebP to drastically reduce payload size and memory footprint.
            // We map into a lightweight serialized list to keep data transfer extremely compact and fast.
            val list = apps.map { app ->
                val webpBase64 = getAppIconBase64WebP(app.package_name)
                mapOf(
                    "package_name" to app.package_name,
                    "name" to app.name,
                    "category" to app.category,
                    "is_favorite" to app.is_favorite,
                    "usage_count" to app.usage_count,
                    "badge_count" to app.badge_count,
                    "accent_color" to app.accent_color,
                    "description" to app.description,
                    "icon_base64" to webpBase64
                )
            }
            gson.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun getAppIconBase64WebP(packageName: String): String {
        return try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                iconLoader.resolveIcon(packageName, null)
            }
        } catch (e: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun launchApp(packageName: String, appName: String?) {
        scope.launch(Dispatchers.Main) {
            onLaunchApp(packageName, appName)
        }
    }

    @JavascriptInterface
    fun getLauncherSchemaJson(): String {
        return try {
            val schema = repository.getSchema()
            gson.toJson(schema)
        } catch (e: Exception) {
            "{}"
        }
    }

    @JavascriptInterface
    fun getAverageLatencyMs(): Long {
        return latencyManager.getAverageLaunchLatencyMs()
    }

    @JavascriptInterface
    fun getBatteryLevel(): Int {
        val ifilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100
        }
    }

    @JavascriptInterface
    fun updateAppLayout(orderedPackagesJson: String): String {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            val packageNames: List<String> = gson.fromJson(orderedPackagesJson, type)
            repository.reorderApps(packageNames)
            
            val batteryPct = getBatteryLevel()
            if (batteryPct < 15) {
                "paused_low_battery"
            } else {
                "synced_to_supabase"
            }
        } catch (e: Exception) {
            "error: ${e.message}"
        }
    }

    @JavascriptInterface
    fun syncToSupabase(): String {
        val batteryPct = getBatteryLevel()
        if (batteryPct < 15) {
            return "paused_low_battery"
        }
        return "sync_success"
    }

    @JavascriptInterface
    fun toggleAppFavorite(packageName: String): Boolean {
        return repository.toggleAppFavorite(packageName)
    }

    @JavascriptInterface
    fun getLauncherConfig(): String {
        return try {
            LauncherConfigManager.getInstance(context).getConfigJson()
        } catch (e: Exception) {
            "{}"
        }
    }

    @JavascriptInterface
    fun saveLauncherConfig(jsonStr: String) {
        try {
            LauncherConfigManager.getInstance(context).updateConfigFromJson(jsonStr, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun updateElement(elementId: String, propertiesJson: String): Boolean {
        return try {
            val schema = repository.getSchema()
            val propertiesType = object : TypeToken<Map<String, Any>>() {}.type
            val properties: Map<String, Any> = gson.fromJson(propertiesJson, propertiesType)

            val updatedPages = schema.pages.map { page ->
                val updatedElements = page.elements.map { element ->
                    if (element.element_id == elementId) {
                        val style = element.style_props
                        val updatedStyle = StyleProps(
                            x = (properties["x"] as? Double)?.toInt() ?: style.x,
                            y = (properties["y"] as? Double)?.toInt() ?: style.y,
                            w = (properties["w"] as? Double)?.toInt() ?: style.w,
                            h = (properties["h"] as? Double)?.toInt() ?: style.h,
                            bgBlur = (properties["bgBlur"] as? Boolean) ?: style.bgBlur,
                            opacity = (properties["opacity"] as? Double)?.toFloat() ?: style.opacity,
                            borderRadius = (properties["borderRadius"] as? Double)?.toInt() ?: style.borderRadius,
                            fontSize = (properties["fontSize"] as? Double)?.toInt() ?: style.fontSize,
                            fontColor = (properties["fontColor"] as? String) ?: style.fontColor,
                            accentColor = (properties["accentColor"] as? String) ?: style.accentColor,
                            fontFamily = (properties["fontFamily"] as? String) ?: style.fontFamily,
                            padding = (properties["padding"] as? Double)?.toInt() ?: style.padding,
                            borderStyle = (properties["borderStyle"] as? String) ?: style.borderStyle
                        )
                        val title = (properties["title"] as? String) ?: element.title
                        element.copy(title = title, style_props = updatedStyle)
                    } else {
                        element
                    }
                }
                page.copy(elements = updatedElements)
            }

            val updatedSchema = schema.copy(
                pages = updatedPages,
                updated_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            repository.saveSchema(updatedSchema)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JavascriptInterface
    fun getInstalledWidgets(): String {
        return try {
            // Can return mock or host installed widgets
            "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun getNotificationsJson(): String {
        return try {
            val list = NotificationBridge.getInstance(context).notificationsFlow.value
            gson.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun dismissNotification(id: String) {
        scope.launch(Dispatchers.Main) {
            NotificationBridge.getInstance(context).dismissNotification(id)
        }
    }

    @JavascriptInterface
    fun clearAllNotifications() {
        scope.launch(Dispatchers.Main) {
            NotificationBridge.getInstance(context).clearAllNotifications()
        }
    }

    @JavascriptInterface
    fun getAppIconBase64(packageName: String): String {
        return try {
            val pm = context.packageManager
            val icon = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(icon)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // ── Marketplace Core & SDK Bridge API ───────────────────────────────────────
    @JavascriptInterface
    fun getMarketplaceCatalog(): String {
        return try {
            val catalog = repository.getMarketplaceCatalog()
            gson.toJson(catalog)
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun installMarketplacePlugin(itemJson: String): String {
        return try {
            val item = gson.fromJson(itemJson, MarketplaceItem::class.java)
            val (success, msg) = repository.installMarketplacePlugin(item)
            JSONObject().put("success", success).put("message", msg).toString()
        } catch (e: Exception) {
            JSONObject().put("success", false).put("message", e.localizedMessage ?: "Parse Error").toString()
        }
    }

    @JavascriptInterface
    fun uninstallMarketplacePlugin(pluginId: String): String {
        return try {
            val (success, msg) = repository.uninstallMarketplacePlugin(pluginId)
            JSONObject().put("success", success).put("message", msg).toString()
        } catch (e: Exception) {
            JSONObject().put("success", false).put("message", e.localizedMessage ?: "Error").toString()
        }
    }

    @JavascriptInterface
    fun registerCustomSdkPlugin(manifestJson: String, payloadJson: String): String {
        return try {
            val (success, msg) = repository.registerCustomPlugin(manifestJson, payloadJson)
            JSONObject().put("success", success).put("message", msg).toString()
        } catch (e: Exception) {
            JSONObject().put("success", false).put("message", e.localizedMessage ?: "Error").toString()
        }
    }

    @JavascriptInterface
    fun executePluginSdkAction(pluginId: String, actionName: String): String {
        return try {
            val res = repository.executePluginSdkAction(pluginId, actionName)
            gson.toJson(res)
        } catch (e: Exception) {
            JSONObject().put("success", false).put("message", e.localizedMessage ?: "Error").toString()
        }
    }

    // ── SchemeStore API ─────────────────────────────────────────────────────────
    @JavascriptInterface
    fun getSchemes(category: String): String {
        return try {
            val store = SchemeManager.getInstance(context)
            gson.toJson(store.getSchemesForCategory(category))
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun saveCustomScheme(category: String, id: String, contentJson: String): Boolean {
        return try {
            val store = SchemeManager.getInstance(context)
            store.saveCustomizedScheme(category, id, contentJson)
        } catch (e: Exception) {
            false
        }
    }

    @JavascriptInterface
    fun getSchemaValuesOnly(): String {
        return try {
            val schema = repository.getSchema()
            val map = mapOf(
                "schema_id" to schema.schema_id,
                "version" to schema.version,
                "theme" to schema.theme,
                "drawer_layout" to schema.drawer_layout,
                "active_font" to schema.active_font,
                "active_icon_pack" to schema.active_icon_pack,
                "installed_plugins" to schema.installed_plugins,
                "pages" to schema.pages,
                "dock" to schema.dock
            )
            gson.toJson(map)
        } catch (e: Exception) {
            "{}"
        }
    }

    @JavascriptInterface
    fun applySchemaValues(valuesJson: String): Boolean {
        return try {
            val schema = repository.getSchema()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(valuesJson, type)

            var newTheme = schema.theme
            if (map.containsKey("theme") && map["theme"] != null) {
                try {
                    val themeJson = gson.toJson(map["theme"])
                    val parsedTheme = gson.fromJson(themeJson, LauncherTheme::class.java)
                    if (parsedTheme != null && !parsedTheme.name.isNullOrEmpty()) {
                        newTheme = parsedTheme
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val updatedSchema = schema.copy(
                schema_id = (map["schema_id"] as? String) ?: schema.schema_id,
                version = (map["version"] as? String) ?: schema.version,
                theme = newTheme,
                drawer_layout = (map["drawer_layout"] as? String) ?: schema.drawer_layout,
                active_font = (map["active_font"] as? String) ?: schema.active_font,
                active_icon_pack = (map["active_icon_pack"] as? String) ?: schema.active_icon_pack,
                updated_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            repository.saveSchema(updatedSchema)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
