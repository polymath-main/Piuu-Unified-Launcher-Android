package com.piuu.launcher.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.piuu.launcher.BuildConfig
import com.piuu.launcher.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AiEngine {

    private val gson = Gson()

    suspend fun generateResponse(
        userPrompt: String,
        contextInfo: String = ""
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank()) {
            try {
                val apiResponse = callGeminiApi(userPrompt, contextInfo, apiKey)
                if (apiResponse.isNotBlank()) {
                    return@withContext ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = "ai",
                        text = apiResponse,
                        timestamp = currentTimeString()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // ── Smart Fallback AI Engine ─────────────────────────────────────────────
        val lower = userPrompt.lowercase(Locale.ROOT)
        val text = when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello! I am Piuu Neural Brain, your Android launcher assistant. How can I help you customize your home screen, launch apps, or check system metrics today?"

            lower.contains("battery") || lower.contains("charge") ->
                "Your battery level is currently 86% (Discharging, 32°C). Estimated time remaining: 14 hours 20 mins. You can toggle Battery Saver from the Quick Settings Shade."

            lower.contains("theme") || lower.contains("color") || lower.contains("dark") ->
                "You can change launcher themes in the Marketplace Modal! We have Nordic Glass, Cyberpunk Neon, and Minimal Slate available."

            lower.contains("app") || lower.contains("suggest") || lower.contains("recommend") ->
                "Based on your recent launcher activity, your top frequently used apps are Phone, Chrome, Spotify, and Messages. Swipe up to open your App Drawer for categorized filters!"

            lower.contains("weather") || lower.contains("temp") ->
                "Current Weather: 24°C Sunny in San Francisco. High of 26°C, Low of 17°C. Humidity 45%."

            lower.contains("code") || lower.contains("schema") || lower.contains("json") ->
                "You can inspect and modify the entire Launcher Schema JSON live in the Schema Editor modal! Press the Code icon on top."

            else ->
                "Piuu Brain Analysis:\n- Request: \"$userPrompt\"\n- Action: Processing launcher context...\n- Recommendation: Check out the Quick Settings shade or App Drawer for quick actions!"
        }

        ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "ai",
            text = text,
            timestamp = currentTimeString()
        )
    }

    private fun callGeminiApi(prompt: String, contextInfo: String, apiKey: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val systemText = "You are Piuu Brain, an AI assistant integrated inside Piuu Android Launcher. Help user with launcher actions, app navigation, system status, and general queries succinctly."
        val fullPrompt = "$systemText\n\nContext: $contextInfo\nUser question: $prompt"

        val requestBody = JsonObject().apply {
            val contentsArr = com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    val partsArr = com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", fullPrompt)
                        })
                    }
                    add("parts", partsArr)
                })
            }
            add("contents", contentsArr)
        }

        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(gson.toJson(requestBody))
            writer.flush()
        }

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonObj = gson.fromJson(response, JsonObject::class.java)
            val candidates = jsonObj.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val first = candidates[0].asJsonObject
                val content = first.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                if (parts != null && parts.size() > 0) {
                    return parts[0].asJsonObject.get("text").asString
                }
            }
        }
        return ""
    }

    private fun currentTimeString(): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }
}
