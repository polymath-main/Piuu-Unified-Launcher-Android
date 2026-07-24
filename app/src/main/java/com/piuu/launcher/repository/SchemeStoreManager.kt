package com.piuu.launcher.repository

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

data class SchemeItem(
    val id: String,
    val name: String,
    val category: String,
    val author: String,
    val rating: Float,
    val downloads: Int,
    val description: String,
    val tag: String,
    val payload: String,
    var isInstalled: Boolean = false
)

class SchemeStoreManager private constructor(private val context: Context) {
    private val baseDir = File(context.filesDir, "SchemeStore")
    private val categories = listOf("themes", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills")

    init {
        ensureDirectoriesAndSchemes()
    }

    private fun ensureDirectoriesAndSchemes() {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        for (category in categories) {
            val catDir = File(baseDir, category)
            if (!catDir.exists()) {
                catDir.mkdirs()
            }
            createDefaultSchemesForCategory(category, catDir)
        }
    }

    private fun createDefaultSchemesForCategory(category: String, dir: File) {
        val schemes = when (category) {
            "themes" -> listOf(
                createSchemeJson("theme_nord", "Nordic Glass", "Sleek frosty blue glassmorphism theme with soft glow borders", "#3B82F6", "#8B5CF6", "rgba(2, 8, 23, 0.85)"),
                createSchemeJson("theme_cyberpunk", "Cyberpunk Neon", "High contrast neon pink and cyan dark theme with glowing cards", "#EC4899", "#06B6D4", "rgba(9, 9, 11, 0.9)"),
                createSchemeJson("theme_slate", "Minimal Slate", "Clean matte dark gray layout for maximum focus and battery saving", "#64748B", "#94A3B8", "rgba(18, 18, 18, 0.92)"),
                createSchemeJson("theme_sunset", "Sunset Retro", "Warm vintage orange and deep purple gradients with grain overlay", "#F97316", "#A855F7", "rgba(30, 17, 42, 0.88)"),
                createSchemeJson("theme_forest", "Forest Moss", "Earthy dark green and beige tones reflecting pristine wild nature", "#10B981", "#F59E0B", "rgba(6, 78, 59, 0.9)"),
                createSchemeJson("theme_sakura", "Sakura Blossom", "Soft cherry blossom pastel pink aesthetics with light transparent panels", "#EC4899", "#F43F5E", "rgba(253, 242, 248, 0.9)"),
                createSchemeJson("theme_solarized_light", "Solarized Light", "High legibility warm light theme using precise solarized color values", "#D97706", "#2563EB", "rgba(253, 253, 250, 0.95)"),
                createSchemeJson("theme_oled", "Midnight OLED", "Pure deep pitch black theme optimized for battery saving on high-end screens", "#000000", "#FFFFFF", "rgba(0, 0, 0, 0.98)")
            )
            "fonts" -> listOf(
                createFontJson("font_inter", "Inter Sans", "Rasmus Andersson", "Highly legible geometric neo-grotesque designed for modern mobile screens", "Inter"),
                createFontJson("font_jakarta", "Plus Jakarta Sans", "Tokotype", "Modern and clean typography with friendly open curves for easy scanning", "Plus Jakarta Sans"),
                createFontJson("font_playfair", "Playfair Serif", "Claus Eggers", "Elegant display serif typeface with high contrast for literary layout styles", "Playfair Display"),
                createFontJson("font_fira", "Fira Code Mono", "Mozilla", "Beautiful monospaced typeface with clean programming ligatures for tech lovers", "Fira Code"),
                createFontJson("font_roboto", "Roboto Standard", "Google", "The classic trusted versatile android geometric standard typeface", "Roboto"),
                createFontJson("font_montserrat", "Montserrat Pro", "Julieta Ulanovsky", "Bold geometric sans-serif inspired by retro posters of Buenos Aires", "Montserrat"),
                createFontJson("font_jetbrains", "JetBrains Mono", "JetBrains", "Specially designed monospace font crafted for high fatigue reduction", "JetBrains Mono")
            )
            "layouts" -> listOf(
                createLayoutJson("layout_categorized", "Smart Categorized", "Piuu Core", "Auto-groups application icons into smart folders using lightweight NLP rules", "categorized"),
                createLayoutJson("layout_vertical", "Classic Vertical Grid", "AOSP Dev", "Traditional vertical scrolling app grid with responsive fling speed", "vertical"),
                createLayoutJson("layout_library", "iOS Style Library", "Cupertino Dev", "Apple-style categorized app tray with dynamic app-search triggers", "ios_library"),
                createLayoutJson("layout_radial", "Radial Swiper", "Futurism Labs", "Aesthetic circular swiper wheel layout optimized for rapid one-hand thumb reach", "radial"),
                createLayoutJson("layout_compact", "Compact Alphabetical", "Minimalist Org", "Ultra clean list index organized alphabetically with index scroll slider", "compact"),
                createLayoutJson("layout_horizontal_paged", "Horizontal Paged", "Launcher Pro", "Multi-page horizontal grid tray reminiscent of traditional launcher pages", "horizontal_paged")
            )
            "faces" -> listOf(
                createFaceJson("face_dashboard", "Dashboard Info Face", "Piuu Design Studio", "Rich informative home screen centering widgets, weather metrics, and devices", "dashboard"),
                createFaceJson("face_analog", "Minimal Analog", "Swiss Chrono", "Ultra-minimal sweeps using simulated vector dials and geometric hour markers", "analog"),
                createFaceJson("face_hud", "Digital Cyber HUD", "NeoTokyo Labs", "Sci-fi interface with moving indicators displaying battery temperature and system load", "cyber_hud"),
                createFaceJson("face_serene", "Nature Zen Calm", "Mindfulness Group", "Abstract geometric circular waveforms pulsing synchronously with breathing cycles", "zen_calm"),
                createFaceJson("face_big_clock", "Big Clock Minimal", "Piuu Core", "Massive elegant vertical clock centering visual negative space", "big_clock"),
                createFaceJson("face_weather", "Weather Forecast Face", "Meteorology Team", "Dynamic animated background depicting active weather patterns inside card frames", "weather")
            )
            "widgets" -> listOf(
                createWidgetJson("widget_battery_pro", "Dynamic Battery Gauge", "Piuu System", "Interactive radial battery gauge mapping charge, health, and current wattage", "battery"),
                createWidgetJson("widget_ai_search", "Neural Search Bar", "Piuu AI", "Advanced search utility leveraging lightweight on-device Gemini assistant triggers", "search"),
                createWidgetJson("widget_resource", "Resource Live Core", "DevTools", "Real-time overlay displaying active CPU usage cycles and active memory consumption", "resources"),
                createWidgetJson("widget_music", "Advanced Music Deck", "AudioWave", "Universal media control deck with sweeping waveform progress meters", "music_deck"),
                createWidgetJson("widget_calendar", "Calendar Schedule", "Piuu Org", "Inline scrolling preview of upcoming task list items and hourly calendar metrics", "calendar"),
                createWidgetJson("widget_world_clock", "World Clock Multi", "AeroTime", "Simultaneous display of three selectable time zones with beautiful high contrast labels", "world_clock")
            )
            "icons" -> listOf(
                createIconJson("icons_neon", "Cyber Glyphs", "NeoTokyo Labs", "Glow outline icons illuminating the screen along core application glyphs", "neon_glyphs"),
                createIconJson("icons_pastel", "Pastel Candy Flat", "SoftVibe Studio", "Warm paper style icons with custom drop shadows and rounded corners", "pastel_candy"),
                createIconJson("icons_monochrome", "Slate Outline Pack", "Monochrome", "Elegant ultra-thin white single-tone outlines on solid glass backgrounds", "mono_slate"),
                createIconJson("icons_pixel", "Pixel Art 8-Bit", "RetroVibe", "Nostalgic handcrafted retro layout pixels depicting standard apps", "pixel_8bit"),
                createIconJson("icons_neon_wire", "Neon Wire Outline", "LightSync", "Animated neon style outlines with custom accent colors on selection", "neon_wire"),
                createIconJson("icons_soft_shadow", "Soft Shadow Material", "Google Design", "Aesthetic material design icons with heavy realistic lighting drop-shadows", "soft_shadow")
            )
            "agents" -> listOf(
                createAgentJson("agent_briefing", "Daily Briefing Bot", "Piuu Brain", "Synthesizes upcoming calendar, local weather, and news into conversational lists", "briefing_agent"),
                createAgentJson("agent_fitness", "Fitness Routine Coach", "Piuu System", "Parses pedometer and run telemetry data to suggest optimized exercise paths", "fitness_agent"),
                createAgentJson("agent_finance", "Crypto & Stock Advisor", "QuantLabs", "Calculates mock daily yields and alerts user about high-momentum stock options", "finance_agent"),
                createAgentJson("agent_zen", "Calm Mindfulness Guide", "Mindfulness Group", "Interactive breathing pace loops and mental wellness focus reminders", "zen_agent"),
                createAgentJson("agent_prod", "Productivity Assistant", "FocusGroup", "Tracks app usage durations to actively trigger app-lock warnings on work times", "prod_agent"),
                createAgentJson("agent_auto", "Task Automator Brain", "AutomateIt", "Creates routine macro scripts based on custom system trigger rules", "auto_agent")
            )
            "skills" -> listOf(
                createSkillJson("agent_coder", "Code Assistant Skill", "DevTools", "Parses schema structure, executes Javascript hooks, and updates styling", "code_assistant"),
                createSkillJson("skill_email", "Email Digest Expert", "Piuu AI", "Consolidates bulky inbox emails into simple bullet action lists", "email_summarizer"),
                createSkillJson("skill_translate", "Live Overlay Translate", "Translatify", "Background translation trigger when text is copied to clipboard", "translator"),
                createSkillJson("skill_automator", "Morning Automation Macro", "Piuu Core", "Adjusts brightness, opens playlist, and disables DND when wake alarm rings", "morning_macro"),
                createSkillJson("skill_news", "News Summarizer Pro", "Piuu AI", "Curates RSS articles matching user's reading preference into summary lists", "news_summarizer"),
                createSkillJson("skill_dictate", "Voice Note Dictator", "AudioScribe", "Highly optimized offline-ready voice transcription widget mapping text to notes", "dictator")
            )
            else -> emptyList()
        }

        for (scheme in schemes) {
            val file = File(dir, "${scheme.first}.json")
            if (!file.exists()) {
                file.writeText(scheme.second)
            }
        }
    }

    private fun createSchemeJson(id: String, name: String, desc: String, primary: String, accent: String, bg: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("description", desc)
        json.put("primary_color", primary)
        json.put("accent_color", accent)
        json.put("bg_overlay", bg)
        json.put("font_family", "Inter")
        return Pair(id, json.toString())
    }

    private fun createFontJson(id: String, name: String, author: String, desc: String, fontName: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("author", author)
        json.put("description", desc)
        json.put("font_family", fontName)
        return Pair(id, json.toString())
    }

    private fun createLayoutJson(id: String, name: String, author: String, desc: String, layoutName: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("author", author)
        json.put("description", desc)
        json.put("layout_name", layoutName)
        return Pair(id, json.toString())
    }

    private fun createFaceJson(id: String, name: String, author: String, desc: String, faceName: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("author", author)
        json.put("description", desc)
        json.put("face_name", faceName)
        return Pair(id, json.toString())
    }

    private fun createWidgetJson(id: String, name: String, author: String, desc: String, widgetName: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("author", author)
        json.put("description", desc)
        json.put("widget_name", widgetName)
        return Pair(id, json.toString())
    }

    private fun createIconJson(id: String, name: String, author: String, desc: String, packName: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("author", author)
        json.put("description", desc)
        json.put("icon_pack_name", packName)
        return Pair(id, json.toString())
    }

    private fun createAgentJson(id: String, name: String, author: String, desc: String, agentName: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("author", author)
        json.put("description", desc)
        json.put("agent_name", agentName)
        return Pair(id, json.toString())
    }

    private fun createSkillJson(id: String, name: String, author: String, desc: String, skillName: String): Pair<String, String> {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("author", author)
        json.put("description", desc)
        json.put("skill_name", skillName)
        return Pair(id, json.toString())
    }

    fun getSchemesForCategory(category: String): List<SchemeItem> {
        val list = mutableListOf<SchemeItem>()
        try {
            val catDir = File(baseDir, category)
            if (catDir.exists()) {
                val files = catDir.listFiles { _, name -> name.endsWith(".json") }
                if (files != null) {
                    for (file in files) {
                        val content = file.readText()
                        val json = JSONObject(content)
                        val id = json.optString("id", file.nameWithoutExtension)
                        val name = json.optString("name", id.replace("_", " ").capitalize())
                        val author = json.optString("author", "Piuu Creator")
                        val description = json.optString("description", "A premium customization scheme")
                        val payload = json.toString()
                        
                        list.add(
                            SchemeItem(
                                id = id,
                                name = name,
                                category = category,
                                author = author,
                                rating = 4.7f,
                                downloads = 12000,
                                description = description,
                                tag = "STORE",
                                payload = payload,
                                isInstalled = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SchemeStoreManager", "Error getting schemes for category: $category", e)
        }
        return list
    }

    fun saveCustomizedScheme(category: String, id: String, contentJson: String): Boolean {
        return try {
            val catDir = File(baseDir, category)
            if (!catDir.exists()) {
                catDir.mkdirs()
            }
            val file = File(catDir, "$id.json")
            file.writeText(contentJson)
            true
        } catch (e: Exception) {
            Log.e("SchemeStoreManager", "Error saving scheme for $category / $id", e)
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SchemeStoreManager? = null

        fun getInstance(context: Context): SchemeStoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SchemeStoreManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
