package com.piuu.launcher.model

import com.google.gson.annotations.SerializedName

enum class ElementType {
    @SerializedName("clock") CLOCK,
    @SerializedName("weather") WEATHER,
    @SerializedName("app_grid") APP_GRID,
    @SerializedName("search_bar") SEARCH_BAR,
    @SerializedName("quick_notes") QUICK_NOTES,
    @SerializedName("media_player") MEDIA_PLAYER,
    @SerializedName("agent_widget") AGENT_WIDGET,
    @SerializedName("custom_card") CUSTOM_CARD,
    @SerializedName("battery_status") BATTERY_STATUS,
    @SerializedName("system_stats") SYSTEM_STATS,
    @SerializedName("piuu_suite_folder") PIUU_SUITE_FOLDER,
    @SerializedName("predictive_suggestions") PREDICTIVE_SUGGESTIONS,
    @SerializedName("notification_center") NOTIFICATION_CENTER
}

data class ActionIntent(
    val type: String, // launch_app, run_agent, open_drawer, toggle_setting, url
    val target: String,
    val params: Map<String, String>? = null
)

data class StyleProps(
    val x: Int? = 0,
    val y: Int? = 0,
    val w: Int? = 2,
    val h: Int? = 2,
    val bgBlur: Boolean? = true,
    val opacity: Float? = 0.8f,
    val borderRadius: Int? = 16,
    val fontSize: Int? = 14,
    val fontColor: String? = "#FFFFFF",
    val accentColor: String? = "#3B82F6",
    val fontFamily: String? = "Inter",
    val padding: Int? = 12,
    val borderStyle: String? = "solid"
)

data class LauncherElement(
    val element_id: String,
    val type: ElementType,
    val title: String? = null,
    val style_props: StyleProps = StyleProps(),
    val action_intent: ActionIntent? = null,
    val data_props: Map<String, Any>? = null
)

data class LauncherPage(
    val page_id: String,
    val title: String,
    val elements: List<LauncherElement>
)

data class LauncherDock(
    val element_id: String,
    val app_packages: List<String>,
    val style_props: StyleProps = StyleProps()
)

data class LauncherTheme(
    val id: String,
    val name: String,
    val author: String,
    val preview_bg: String,
    val wallpaper_url: String,
    val is_dark: Boolean = true,
    val glass_blur: Boolean = true,
    val primary_color: String = "#3B82F6",
    val accent_color: String = "#8B5CF6",
    val bg_overlay: String = "rgba(2, 8, 23, 0.8)",
    val font_family: String = "Inter",
    val border_radius: Int = 20,
    val card_opacity: Float = 0.85f,
    val card_glass: String = "#304159",
    val icon_pack_id: String = "default"
)

data class LauncherSchema(
    val schema_id: String,
    val version: String,
    val updated_at: String,
    val theme: LauncherTheme,
    val pages: List<LauncherPage>,
    val dock: LauncherDock,
    val drawer_layout: String = "categorized", // categorized, vertical, radial, ios_library
    val active_font: String = "Inter",
    val active_icon_pack: String = "default",
    val installed_plugins: List<String> = emptyList()
)

data class SystemApp(
    val package_name: String,
    val name: String,
    val icon_name: String,
    val category: String, // social, productivity, entertainment, system, utilities, finance, creative, piuu_suite
    val launch_intent: String,
    var usage_count: Int = 0,
    val badge_count: Int = 0,
    var is_favorite: Boolean = false,
    val accent_color: String = "#3B82F6",
    val description: String = ""
)

data class MarketplaceItem(
    val id: String,
    val name: String,
    val category: String, // theming, fonts, drawer, widgets, icons, agents, skills
    val author: String,
    val rating: Float,
    val downloads: Int,
    val description: String,
    val preview_badge: String? = null,
    val payload: String? = null,
    var is_installed: Boolean = false
)

data class AgentDefinition(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val trigger_condition: String,
    val system_prompt: String,
    val skills: List<String> = emptyList(),
    val avatar_color: String = "#8B5CF6"
)

data class SystemMetrics(
    val cpu_usage: Int = 28,
    val ram_used_mb: Float = 3.4f,
    val ram_total_mb: Float = 8.0f,
    val battery_pct: Int = 86,
    val battery_temp: Int = 32,
    val storage_used_gb: Float = 42.1f,
    val storage_total_gb: Float = 128.0f,
    val network_speed_kbps: Float = 1240.0f,
    val is_charging: Boolean = false,
    val wifi_enabled: Boolean = true,
    val bluetooth_enabled: Boolean = true,
    val flashlight_enabled: Boolean = false,
    val battery_saver: Boolean = false,
    val do_not_disturb: Boolean = false
)

data class NotificationItem(
    val id: String,
    val app_name: String,
    val title: String,
    val message: String,
    val time: String,
    val icon_name: String,
    var is_read: Boolean = false
)

data class ChatMessage(
    val id: String,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String,
    val is_code: Boolean = false,
    val code_content: String? = null
)

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: String, // "app", "contact", "note", "setting", "web"
    val target: String,
    val icon_name: String = "search"
)

data class NoteItem(
    val id: String,
    val content: String,
    val timestamp: String,
    val color: String = "#3B82F6"
)
