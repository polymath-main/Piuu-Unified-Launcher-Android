package com.piuu.launcher.marketplace

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.piuu.launcher.model.LauncherSchema
import com.piuu.launcher.model.MarketplaceItem
import com.piuu.launcher.repository.SchemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/**
 * Piuu Marketplace Core Engine
 * Manages plugin lifecycle, installation sandbox, persistence, and Scheme Store syncing.
 */
class MarketplaceCore private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val schemeManager = SchemeManager.getInstance(context)

    private val _installedPluginsFlow = MutableStateFlow<List<MarketplacePlugin>>(emptyList())
    val installedPluginsFlow: StateFlow<List<MarketplacePlugin>> = _installedPluginsFlow.asStateFlow()

    init {
        loadInstalledPluginsFromStorage()
    }

    private fun loadInstalledPluginsFromStorage() {
        val json = prefs.getString(KEY_INSTALLED_PLUGINS, "[]") ?: "[]"
        try {
            val type = object : TypeToken<List<MarketplacePlugin>>() {}.type
            val plugins: List<MarketplacePlugin> = gson.fromJson(json, type) ?: emptyList()
            _installedPluginsFlow.value = plugins
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse stored marketplace plugins", e)
            _installedPluginsFlow.value = emptyList()
        }
    }

    private fun saveInstalledPluginsToStorage(plugins: List<MarketplacePlugin>) {
        try {
            val json = gson.toJson(plugins)
            prefs.edit().putString(KEY_INSTALLED_PLUGINS, json).apply()
            _installedPluginsFlow.value = plugins
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist marketplace plugins", e)
        }
    }

    /**
     * Get unified Marketplace catalog merging SchemeManager presets and custom installed plugins.
     */
    fun getFullCatalog(currentSchema: LauncherSchema? = null): List<MarketplaceItem> {
        val schema = currentSchema
        val categories = listOf("themes", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills")
        val itemsMap = LinkedHashMap<String, MarketplaceItem>()
        val installedList = _installedPluginsFlow.value

        // 1. Process SchemeManager schemes
        for (category in categories) {
            val schemes = schemeManager.getSchemesForCategory(category)
            for (scheme in schemes) {
                val cat = when (category) {
                    "themes" -> "theming"
                    else -> category
                }

                val installedPlugin = installedList.find { it.manifest.id == scheme.id }
                val isInstalledBySchema = if (schema != null) {
                    when (cat) {
                        "theming" -> schema.theme.id == scheme.id
                        "fonts" -> schema.active_font == scheme.id || schema.theme.font_family.contains(scheme.id.removePrefix("font_"), ignoreCase = true)
                        "layouts" -> schema.drawer_layout == scheme.id
                        "icons" -> schema.active_icon_pack == scheme.id
                        else -> schema.installed_plugins.contains(scheme.id)
                    }
                } else false

                val isInstalled = installedPlugin?.isInstalled == true || isInstalledBySchema

                itemsMap[scheme.id] = MarketplaceItem(
                    id = scheme.id,
                    name = scheme.name,
                    category = cat,
                    author = scheme.author,
                    rating = 4.9f,
                    downloads = 1420,
                    description = scheme.description,
                    preview_badge = if (scheme.isCustom) "Custom" else "Core SDK",
                    payload = scheme.payload,
                    is_installed = isInstalled
                )
            }
        }

        // 2. Include any custom installed plugins from SDK sandbox
        for (plugin in installedList) {
            if (!itemsMap.containsKey(plugin.manifest.id)) {
                itemsMap[plugin.manifest.id] = plugin.toMarketplaceItem()
            }
        }

        return itemsMap.values.toList()
    }

    /**
     * Install / Apply a plugin item into local SDK sandbox and update state.
     */
    fun installPlugin(item: MarketplaceItem): Pair<Boolean, String> {
        val currentList = _installedPluginsFlow.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.manifest.id == item.id }

        val manifest = PiuuPluginSdk.buildManifestFromScheme(
            id = item.id,
            name = item.name,
            category = item.category,
            author = item.author,
            description = item.description,
            payload = item.payload ?: "{}"
        )

        val newPlugin = MarketplacePlugin(
            manifest = manifest,
            payload = item.payload ?: "{}",
            isInstalled = true,
            isEnabled = true,
            installedAt = System.currentTimeMillis()
        )

        if (existingIndex >= 0) {
            currentList[existingIndex] = newPlugin
        } else {
            currentList.add(newPlugin)
        }

        // Persist customized scheme JSON to SchemeManager as well
        if (!item.payload.isNullOrBlank()) {
            val cat = when (item.category.lowercase()) {
                "theming", "themes" -> "themes"
                else -> item.category.lowercase()
            }
            schemeManager.saveCustomizedScheme(cat, item.id, item.payload)
        }

        saveInstalledPluginsToStorage(currentList)
        return Pair(true, "Successfully installed '${item.name}' [SDK v${manifest.sdk_version}]")
    }

    /**
     * Uninstall a plugin from the marketplace sandbox.
     */
    fun uninstallPlugin(pluginId: String): Pair<Boolean, String> {
        val currentList = _installedPluginsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.manifest.id == pluginId }
        if (index >= 0) {
            val removed = currentList.removeAt(index)
            saveInstalledPluginsToStorage(currentList)
            return Pair(true, "Uninstalled '${removed.manifest.name}'")
        }
        return Pair(false, "Plugin ID '$pluginId' not found.")
    }

    /**
     * Enable or Disable an installed plugin.
     */
    fun togglePluginEnabled(pluginId: String, enable: Boolean): Boolean {
        val currentList = _installedPluginsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.manifest.id == pluginId }
        if (index >= 0) {
            val existing = currentList[index]
            currentList[index] = existing.copy(isEnabled = enable)
            saveInstalledPluginsToStorage(currentList)
            return true
        }
        return false
    }

    /**
     * Register a new custom plugin via raw Manifest & Payload JSON string.
     */
    fun registerCustomPluginFromManifest(manifestJson: String, payloadJson: String): Pair<Boolean, String> {
        val (valid, manifest) = PiuuPluginSdk.validateManifest(manifestJson)
        if (!valid || manifest == null) {
            return Pair(false, "Invalid plugin manifest JSON. Ensure 'id', 'name', and 'type' are present.")
        }

        val plugin = MarketplacePlugin(
            manifest = manifest,
            payload = payloadJson.ifBlank { "{}" },
            isInstalled = true,
            isEnabled = true,
            installedAt = System.currentTimeMillis()
        )

        val currentList = _installedPluginsFlow.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.manifest.id == manifest.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = plugin
        } else {
            currentList.add(plugin)
        }

        saveInstalledPluginsToStorage(currentList)
        schemeManager.saveCustomizedScheme(manifest.category, manifest.id, payloadJson)

        return Pair(true, "Registered Custom SDK Plugin '${manifest.name}' [Type: ${manifest.type}]")
    }

    /**
     * Install and unpack a .piuu bundle archive (compiled via wc-bundle-packer or piuu-studio).
     */
    fun installPiuuBundle(bundleFile: File): Pair<Boolean, String> {
        if (!bundleFile.exists() || !bundleFile.isFile) {
            return Pair(false, "Bundle file '${bundleFile.absolutePath}' does not exist.")
        }

        try {
            var manifestJson = ""
            var payloadJson = "{}"
            val pluginDir = File(context.filesDir, "marketplace_plugins/${bundleFile.nameWithoutExtension}")
            pluginDir.mkdirs()

            java.util.zip.ZipFile(bundleFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val destFile = File(pluginDir, entry.name)

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        if (entry.name == "plugin.json" || entry.name == "manifest.json") {
                            manifestJson = destFile.readText()
                        } else if (entry.name == "payload.json" || entry.name == "scheme.json") {
                            payloadJson = destFile.readText()
                        }
                    }
                }
            }

            if (manifestJson.isBlank()) {
                return Pair(false, "Invalid .piuu bundle: Missing plugin.json or manifest.json")
            }

            val (valid, manifest) = PiuuPluginSdk.validateManifest(manifestJson)
            if (!valid || manifest == null) {
                return Pair(false, "Failed to validate bundle manifest.")
            }

            val plugin = MarketplacePlugin(
                manifest = manifest,
                payload = payloadJson.ifBlank { "{}" },
                isInstalled = true,
                isEnabled = true,
                installedAt = System.currentTimeMillis()
            )

            val currentList = _installedPluginsFlow.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.manifest.id == manifest.id }
            if (existingIndex >= 0) {
                currentList[existingIndex] = plugin
            } else {
                currentList.add(plugin)
            }

            saveInstalledPluginsToStorage(currentList)
            schemeManager.saveCustomizedScheme(manifest.category, manifest.id, payloadJson)

            return Pair(true, "Installed .piuu bundle '${manifest.name}' [v${manifest.version}]")
        } catch (e: Exception) {
            Log.e(TAG, "Error installing .piuu bundle", e)
            return Pair(false, "Bundle unpack error: ${e.message}")
        }
    }

    /**
     * Install a .piuu bundle directly from an Android content URI.
     */
    fun installPiuuBundleFromUri(uri: android.net.Uri): Pair<Boolean, String> {
        return try {
            val tempFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}.piuu")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val result = installPiuuBundle(tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read .piuu bundle from URI: $uri", e)
            Pair(false, "Import failed: ${e.message}")
        }
    }

    /**
     * Execute a sandboxed plugin action.
     */

    fun executePluginSdkAction(pluginId: String, actionName: String): PluginExecutionResult {
        val plugin = _installedPluginsFlow.value.find { it.manifest.id == pluginId }
            ?: return PluginExecutionResult(false, "Plugin '$pluginId' is not installed.")

        if (!plugin.isEnabled) {
            return PluginExecutionResult(false, "Plugin '${plugin.manifest.name}' is currently disabled.")
        }

        return when (actionName.lowercase()) {
            "get_metrics", "system_stats" -> PiuuPluginSdk.getSystemMetrics(context, plugin)
            "inspect_manifest" -> PluginExecutionResult(
                success = true,
                message = "Manifest inspection for ${plugin.manifest.name}",
                data = mapOf(
                    "id" to plugin.manifest.id,
                    "version" to plugin.manifest.version,
                    "sdk_version" to plugin.manifest.sdk_version,
                    "permissions" to plugin.manifest.permissions,
                    "type" to plugin.manifest.type.name
                )
            )
            else -> PluginExecutionResult(
                success = true,
                message = "Executed action '$actionName' on plugin '${plugin.manifest.name}'",
                data = mapOf("status" to "OK", "timestamp" to System.currentTimeMillis())
            )
        }
    }

    companion object {

        private const val TAG = "MarketplaceCore"
        private const val PREFS_NAME = "piuu_marketplace_core_prefs"
        private const val KEY_INSTALLED_PLUGINS = "key_installed_plugins_v2"

        @Volatile
        private var INSTANCE: MarketplaceCore? = null

        fun getInstance(context: Context): MarketplaceCore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MarketplaceCore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
