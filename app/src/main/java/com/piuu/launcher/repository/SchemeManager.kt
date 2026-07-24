package com.piuu.launcher.repository

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

data class SchemeConfig(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val category: String, // "themes", "icons", "layouts"
    val payload: String,
    val isCustom: Boolean
)

class SchemeManager private constructor(private val context: Context) {
    private val customBaseDir = File(context.filesDir, "SchemeStore")

    init {
        ensureCustomDirectories()
    }

    private fun ensureCustomDirectories() {
        if (!customBaseDir.exists()) {
            customBaseDir.mkdirs()
        }
        val categories = listOf("themes", "fonts", "layouts", "faces", "widgets", "icons", "agents", "skills")
        categories.forEach { subDir ->
            val dir = File(customBaseDir, subDir)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    /**
     * Fetch all schemes of a specific category from assets and user's custom folder.
     */
    fun getSchemesForCategory(category: String): List<SchemeConfig> {
        val schemes = mutableMapOf<String, SchemeConfig>()

        // 1. Load pre-built schemes from Android Assets
        try {
            val assetPath = "SchemeStore/$category"
            val assetFiles = context.assets.list(assetPath)
            if (assetFiles != null) {
                for (fileName in assetFiles) {
                    if (fileName.endsWith(".json")) {
                        try {
                            val content = context.assets.open("$assetPath/$fileName").bufferedReader().use { it.readText() }
                            val json = JSONObject(content)
                            val id = json.optString("id", fileName.removeSuffix(".json"))
                            val name = json.optString("name", id.replace("_", " ").capitalize())
                            val author = json.optString("author", "Piuu System")
                            val description = json.optString("description", "A premium system scheme")

                            schemes[id] = SchemeConfig(
                                id = id,
                                name = name,
                                author = author,
                                description = description,
                                category = category,
                                payload = content,
                                isCustom = false
                            )
                        } catch (e: Exception) {
                            Log.e("SchemeManager", "Error reading asset scheme $fileName", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SchemeManager", "Error listing asset schemes for $category", e)
        }

        // 2. Load custom user-saved schemes from internal storage (overriding or supplementing)
        try {
            val customDir = File(customBaseDir, category)
            if (customDir.exists()) {
                val files = customDir.listFiles { _, name -> name.endsWith(".json") }
                if (files != null) {
                    for (file in files) {
                        try {
                            val content = file.readText()
                            val json = JSONObject(content)
                            val id = json.optString("id", file.nameWithoutExtension)
                            val name = json.optString("name", id.replace("_", " ").capitalize())
                            val author = json.optString("author", "User Custom")
                            val description = json.optString("description", "A customized user scheme")

                            schemes[id] = SchemeConfig(
                                id = id,
                                name = name,
                                author = author,
                                description = description,
                                category = category,
                                payload = content,
                                isCustom = true
                            )
                        } catch (e: Exception) {
                            Log.e("SchemeManager", "Error reading custom scheme file ${file.name}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SchemeManager", "Error reading custom schemes for $category", e)
        }

        return schemes.values.toList()
    }

    /**
     * Save a customized user scheme.
     */
    fun saveCustomizedScheme(category: String, id: String, contentJson: String): Boolean {
        return try {
            val customDir = File(customBaseDir, category)
            if (!customDir.exists()) {
                customDir.mkdirs()
            }
            val file = File(customDir, "$id.json")
            file.writeText(contentJson)
            true
        } catch (e: Exception) {
            Log.e("SchemeManager", "Error saving custom scheme for $category / $id", e)
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SchemeManager? = null

        fun getInstance(context: Context): SchemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SchemeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
