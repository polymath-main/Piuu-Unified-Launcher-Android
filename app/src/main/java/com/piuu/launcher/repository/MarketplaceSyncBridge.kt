package com.piuu.launcher.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.piuu.launcher.model.AtomicElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MarketplaceSyncBridge(private val context: Context) {

    private val gson = Gson()
    private val atomicMatrixFile = File(context.filesDir, "ui_atomic_matrix.json")

    /**
     * Serializes the user's current atomic elements configuration into a unified JSON string.
     */
    suspend fun serializeCurrentSetup(): String = withContext(Dispatchers.IO) {
        try {
            if (!atomicMatrixFile.exists()) {
                return@withContext gson.toJson(emptyList<AtomicElement>())
            }
            val jsonContent = atomicMatrixFile.readText()
            jsonContent
        } catch (e: Exception) {
            Log.e("MarketplaceSyncBridge", "Failed to serialize layout configuration", e)
            gson.toJson(emptyList<AtomicElement>())
        }
    }

    /**
     * Deserializes a schema configuration JSON and saves it locally to override the current layout.
     */
    suspend fun deserializeAndApplySetup(configJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Verify and parse to ensure the schema is valid
            val typeToken = object : TypeToken<List<AtomicElement>>() {}.type
            val elements: List<AtomicElement> = gson.fromJson(configJson, typeToken)
            
            // Save locally
            atomicMatrixFile.writeText(configJson)
            true
        } catch (e: Exception) {
            Log.e("MarketplaceSyncBridge", "Failed to deserialize and apply layout configuration", e)
            false
        }
    }

    /**
     * Uploads the serialized configuration directly to the Marketplace cloud database.
     */
    suspend fun uploadSetupToCloud(userId: String, marketplaceApiUrl: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val payload = mapOf(
                "userId" to userId,
                "timestamp" to System.currentTimeMillis(),
                "config" to serializeCurrentSetup()
            )
            val requestBody = gson.toJson(payload)

            val url = URL(marketplaceApiUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            connection.outputStream.use { os ->
                val input = requestBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED
        } catch (e: Exception) {
            Log.e("MarketplaceSyncBridge", "Error syncing configuration to cloud backend", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Downloads a layout package from the Marketplace cloud database.
     */
    suspend fun downloadSetupFromCloud(userId: String, marketplaceApiUrl: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$marketplaceApiUrl?userId=$userId")
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                // Parse and apply locally
                deserializeAndApplySetup(responseText)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MarketplaceSyncBridge", "Error fetching layout setup from cloud backend", e)
            false
        } finally {
            connection?.disconnect()
        }
    }
}
