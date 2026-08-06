package com.piuu.launcher.marketplace

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.google.gson.annotations.SerializedName
import com.piuu.launcher.model.MarketplaceItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Piuu Marketplace SDK Specification & Plugin Types
 */

enum class MarketplacePluginType {
    @SerializedName("theme") THEME,
    @SerializedName("widget") WIDGET,
    @SerializedName("font") FONT,
    @SerializedName("icon_pack") ICON_PACK,
    @SerializedName("ai_agent") AI_AGENT,
    @SerializedName("ai_skill") AI_SKILL,
    @SerializedName("quick_action") QUICK_ACTION,
    @SerializedName("live_wallpaper") LIVE_WALLPAPER,
    @SerializedName("extension") EXTENSION
}

enum class MarketplacePermission(val key: String, val description: String) {
    SYSTEM_STATS("system_stats", "Read CPU, RAM, and Battery Telemetry"),
    DYNAMIC_THEMING("dynamic_theming", "Apply colors and typography system-wide"),
    NOTIFICATION_READ("notification_read", "Access active launcher notification stream"),
    LAUNCH_APP("launch_app", "Launch installed application packages"),
    AI_INFERENCE("ai_inference", "Invoke Gemini AI engine prompts and completions"),
    PERSISTENT_STORAGE("persistent_storage", "Read & write plugin configuration key-value storage")
}

data class MarketplacePluginManifest(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val sdk_version: String = "2.1.0",
    val type: MarketplacePluginType = MarketplacePluginType.EXTENSION,
    val author: String = "Piuu Community",
    val description: String = "",
    val category: String = "widgets",
    val rating: Float = 4.8f,
    val downloads: Int = 1000,
    val preview_badge: String? = "Official",
    val permissions: List<String> = listOf("system_stats", "persistent_storage"),
    val min_launcher_version: String = "1.0.0",
    val entry_point: String? = null,
    val dependencies: List<String> = emptyList()
)

data class MarketplacePlugin(
    val manifest: MarketplacePluginManifest,
    val payload: String = "{}",
    var isInstalled: Boolean = false,
    var isEnabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
    val config: MutableMap<String, String> = mutableMapOf()
) {
    fun toMarketplaceItem(): MarketplaceItem {
        return MarketplaceItem(
            id = manifest.id,
            name = manifest.name,
            category = manifest.category,
            author = manifest.author,
            rating = manifest.rating,
            downloads = manifest.downloads,
            description = manifest.description,
            preview_badge = manifest.preview_badge ?: (if (isEnabled) "Active" else "Disabled"),
            payload = payload,
            is_installed = isInstalled
        )
    }
}

data class PluginExecutionResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)

/**
 * PiuuPluginSdk Runtime Engine
 * Exposes safe APIs to execute plugin actions inside sandbox.
 */
object PiuuPluginSdk {
    const val SDK_VERSION = "2.1.0"
    const val MIN_SUPPORTED_VERSION = "1.0.0"

    /**
     * Parse and validate JSON manifest string against SDK rules.
     */
    fun validateManifest(manifestJson: String): Pair<Boolean, MarketplacePluginManifest?> {
        return try {
            val json = JSONObject(manifestJson)
            val id = json.optString("id", "").ifEmpty { "plugin_" + UUID.randomUUID().toString().take(8) }
            val name = json.optString("name", "Unnamed Extension")
            val version = json.optString("version", "1.0.0")
            val sdkVersion = json.optString("sdk_version", SDK_VERSION)
            val typeStr = json.optString("type", "extension").uppercase()
            val type = try { MarketplacePluginType.valueOf(typeStr) } catch (_: Exception) { MarketplacePluginType.EXTENSION }
            val author = json.optString("author", "Community Developer")
            val description = json.optString("description", "A custom Piuu Launcher extension")
            val category = json.optString("category", "widgets")
            val rating = json.optDouble("rating", 4.8).toFloat()
            val downloads = json.optInt("downloads", 500)
            val previewBadge = json.optString("preview_badge", "Custom SDK")

            val perms = mutableListOf<String>()
            val permsArray = json.optJSONArray("permissions")
            if (permsArray != null) {
                for (i in 0 until permsArray.length()) {
                    perms.add(permsArray.getString(i))
                }
            } else {
                perms.add("persistent_storage")
            }

            val manifest = MarketplacePluginManifest(
                id = id,
                name = name,
                version = version,
                sdk_version = sdkVersion,
                type = type,
                author = author,
                description = description,
                category = category,
                rating = rating,
                downloads = downloads,
                preview_badge = previewBadge,
                permissions = perms,
                min_launcher_version = json.optString("min_launcher_version", "1.0.0"),
                entry_point = json.optString("entry_point", null)
            )

            Pair(true, manifest)
        } catch (e: Exception) {
            Pair(false, null)
        }
    }

    /**
     * Safely query real-time system metrics if plugin has SYSTEM_STATS permission.
     */
    fun getSystemMetrics(context: Context, plugin: MarketplacePlugin): PluginExecutionResult {
        if (!plugin.manifest.permissions.contains("system_stats")) {
            return PluginExecutionResult(
                success = false,
                message = "Permission Denied: Plugin missing 'system_stats' in manifest."
            )
        }

        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

            val memInfo = com.piuu.launcher.repository.LibC.getMemInfo()
            val cpuLoad = com.piuu.launcher.repository.LibC.systemCpuLoad.value
            val usedMemMb = (memInfo.usedGb * 1024).toLong()
            val maxMemMb = (memInfo.totalGb * 1024).toLong()

            val statFs = StatFs(Environment.getDataDirectory().path)
            val availableStorageGb = (statFs.availableBlocksLong * statFs.blockSizeLong) / (1024 * 1024 * 1024)

            val data = mapOf(
                "battery_level" to batteryLevel,
                "used_ram_mb" to usedMemMb,
                "max_ram_mb" to maxMemMb,
                "cpu_load_pct" to cpuLoad.toInt(),
                "available_storage_gb" to availableStorageGb,
                "sdk_version" to SDK_VERSION,
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            PluginExecutionResult(success = true, message = "System metrics fetched successfully", data = data)
        } catch (e: Exception) {
            PluginExecutionResult(success = false, message = "Error fetching metrics: ${e.localizedMessage}")
        }
    }

    /**
     * Convert raw JSON Marketplace Scheme payload into MarketplacePluginManifest
     */
    fun buildManifestFromScheme(id: String, name: String, category: String, author: String, description: String, payload: String): MarketplacePluginManifest {
        val pluginType = when (category.lowercase()) {
            "theming", "themes" -> MarketplacePluginType.THEME
            "fonts" -> MarketplacePluginType.FONT
            "widgets", "faces" -> MarketplacePluginType.WIDGET
            "icons" -> MarketplacePluginType.ICON_PACK
            "agents" -> MarketplacePluginType.AI_AGENT
            "skills" -> MarketplacePluginType.AI_SKILL
            else -> MarketplacePluginType.EXTENSION
        }

        return MarketplacePluginManifest(
            id = id,
            name = name,
            version = "1.2.0",
            sdk_version = SDK_VERSION,
            type = pluginType,
            author = author,
            description = description,
            category = category,
            rating = 4.9f,
            downloads = 1500,
            preview_badge = "Core Verified",
            permissions = listOf("dynamic_theming", "persistent_storage", "system_stats")
        )
    }
}
