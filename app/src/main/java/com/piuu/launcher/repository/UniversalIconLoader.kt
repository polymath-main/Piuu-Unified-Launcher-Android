package com.piuu.launcher.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.piuu.launcher.marketplace.MarketplaceCore
import java.io.ByteArrayOutputStream
import java.io.File

class UniversalIconLoader(private val context: Context) {
    private val preferenceManager = LauncherPreferenceManager.getInstance(context)
    private val iconPackManager = IconPackManager(context)
    private val pm = context.packageManager

    // Memory cache for Base64 WebP strings (max 100 entries)
    private val memoryCache = LruCache<String, String>(100)

    // Disk cache directory
    private val diskCacheDir = File(context.cacheDir, "icon_disk_cache").apply {
        if (!exists()) mkdirs()
    }

    private fun getActiveIconPackAndVersion(): Pair<String, String> {
        try {
            val repository = LauncherRepository(context)
            val schema = repository.getSchema()
            val activeIconPack = schema.active_icon_pack
            if (activeIconPack.isEmpty() || activeIconPack.equals("default", ignoreCase = true)) {
                return Pair("default", "1.0.0")
            }

            // Look up version in installed plugins
            val marketplaceCore = MarketplaceCore.getInstance(context)
            val installedPlugins = marketplaceCore.installedPluginsFlow.value
            val plugin = installedPlugins.find { it.manifest.id.equals(activeIconPack, ignoreCase = true) }
            if (plugin != null) {
                return Pair(activeIconPack, plugin.manifest.version)
            }

            // Look up in SchemeStore
            val schemeManager = SchemeManager.getInstance(context)
            val schemes = schemeManager.getSchemesForCategory("icons")
            val scheme = schemes.find { it.id.equals(activeIconPack, ignoreCase = true) }
            if (scheme != null) {
                val json = JSONObject(scheme.payload)
                return Pair(activeIconPack, json.optString("version", "1.0.0"))
            }

            return Pair(activeIconPack, "1.0.0")
        } catch (e: Exception) {
            return Pair("default", "1.0.0")
        }
    }

    private fun getDiskCachedIcon(packageName: String, activityName: String? = null, currentPack: String, currentVersion: String): String? {
        val sanitizedKey = "${packageName.replace(".", "_")}_${activityName?.replace(".", "_") ?: "main"}"
        val cacheFile = File(diskCacheDir, "${sanitizedKey}.cache")
        if (!cacheFile.exists()) return null

        try {
            cacheFile.bufferedReader().use { reader ->
                val cachedHeader = reader.readLine() ?: return null
                val expectedHeader = "v1:${currentPack}:${currentVersion}"
                if (cachedHeader == expectedHeader) {
                    return reader.readText() // Read the rest of the file (Base64 WebP string)
                }
            }
        } catch (e: Exception) {
            Log.e("UniversalIconLoader", "Failed to read disk cache for $packageName", e)
        }
        return null
    }

    private fun saveDiskCachedIcon(packageName: String, activityName: String? = null, currentPack: String, currentVersion: String, base64Str: String) {
        val sanitizedKey = "${packageName.replace(".", "_")}_${activityName?.replace(".", "_") ?: "main"}"
        val cacheFile = File(diskCacheDir, "${sanitizedKey}.cache")
        try {
            cacheFile.bufferedWriter().use { writer ->
                writer.write("v1:${currentPack}:${currentVersion}\n")
                writer.write(base64Str)
            }
        } catch (e: Exception) {
            Log.e("UniversalIconLoader", "Failed to write disk cache for $packageName", e)
        }
    }

    suspend fun resolveIcon(packageName: String, activityName: String? = null): String = withContext(Dispatchers.IO) {
        val cacheKey = "$packageName/$activityName"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        val (currentPack, currentVersion) = getActiveIconPackAndVersion()
        getDiskCachedIcon(packageName, activityName, currentPack, currentVersion)?.let { cachedBase64 ->
            memoryCache.put(cacheKey, cachedBase64)
            return@withContext cachedBase64
        }

        val iconBitmap = resolveBitmap(packageName, activityName) ?: throw Exception("Icon not found for $packageName")
        val scaled = Bitmap.createScaledBitmap(iconBitmap, 128, 128, true)
        if (scaled != iconBitmap && !iconBitmap.isRecycled) {
            iconBitmap.recycle()
        }

        val outputStream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, outputStream)
        } else {
            @Suppress("DEPRECATION")
            scaled.compress(Bitmap.CompressFormat.WEBP, 80, outputStream)
        }
        val byteArray = outputStream.toByteArray()
        if (!scaled.isRecycled) {
            scaled.recycle()
        }

        val base64Str = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        memoryCache.put(cacheKey, base64Str)
        saveDiskCachedIcon(packageName, activityName, currentPack, currentVersion, base64Str)
        base64Str
    }

    private fun resolveBitmap(packageName: String, activityName: String?): Bitmap? {
        // Layer 1: Check Icon Pack
        try {
            val repository = LauncherRepository(context)
            val schema = repository.getSchema()
            val activePack = schema.active_icon_pack
            if (activePack.isNotEmpty() && activePack != "default" && activePack != "system") {
                if (iconPackManager.loadIconPack(activePack)) {
                    val packBitmap = iconPackManager.loadIconFromPack(packageName, activityName)
                    if (packBitmap != null) {
                        return packBitmap
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UniversalIconLoader", "Failed to load schema icon pack", e)
        }

        val packPkg = preferenceManager.iconPackPackageName
        if (packPkg != "system" && packPkg.isNotEmpty()) {
            iconPackManager.loadIconPack(packPkg)
            val packBitmap = iconPackManager.loadIconFromPack(packageName, activityName)
            if (packBitmap != null) {
                return packBitmap
            }
        }

        // Layer 2: Check Local Store (context.filesDir/custom_icons/{packageName}.webp)
        try {
            val customDir = File(context.filesDir, "custom_icons")
            val customFile = File(customDir, "$packageName.webp")
            if (customFile.exists()) {
                val localBitmap = BitmapFactory.decodeFile(customFile.absolutePath)
                if (localBitmap != null) {
                    return localBitmap
                }
            }
        } catch (e: Exception) {
            // Fallback to system if local read fails
        }

        // Layer 3: Fallback to System
        return try {
            val drawable = pm.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

// Helper utilities for Jetpack Compose integration of adaptive base64 icons
@Composable
fun AppIcon(
    packageName: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    iconBase64: String? = null,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(22),
    fallbackIconName: String = "apps",
    tintColor: androidx.compose.ui.graphics.Color? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val iconLoader = remember { UniversalIconLoader(context) }
    val repository = remember { LauncherRepository(context) }
    val schema = repository.getSchema()
    val activeIconPack = schema.active_icon_pack.lowercase()
    val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
    
    val chosenShape = remember(prefManager.adaptiveIconShape) {
        when (prefManager.adaptiveIconShape.uppercase()) {
            "CIRCLE" -> androidx.compose.foundation.shape.CircleShape
            "SQUIRCLE" -> androidx.compose.foundation.shape.RoundedCornerShape(32)
            "TEARDROP" -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            "ROUNDED_RECT" -> androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            else -> androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        }
    }
    
    val iconBitmapState = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, 
        key1 = packageName, 
        key2 = iconBase64,
        key3 = activeIconPack
    ) {
        try {
            val rawBase64 = if (!iconBase64.isNullOrEmpty()) {
                if (iconBase64.contains(",")) iconBase64.substringAfter(",") else iconBase64
            } else {
                iconLoader.resolveIcon(packageName)
            }
            if (rawBase64.length > 20) {
                val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                value = bitmap?.asImageBitmap()
            }
        } catch (_: Exception) {}
    }

    val bitmap = iconBitmapState.value
    val isThemed = activeIconPack != "default" && activeIconPack.isNotEmpty()

    if (isThemed) {
        val packShape = when {
            activeIconPack.contains("pixel") -> androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            activeIconPack.contains("pastel") -> androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            activeIconPack.contains("monochrome") || activeIconPack.contains("neon") || activeIconPack.contains("gold") -> androidx.compose.foundation.shape.CircleShape
            else -> chosenShape
        }

        val packModifier = modifier
            .size(48.dp)
            .clip(packShape)
            .then(
                when {
                    activeIconPack.contains("neon") -> {
                        androidx.compose.ui.Modifier
                            .background(androidx.compose.ui.graphics.Color(0xFF0F172A))
                            .border(1.5.dp, androidx.compose.ui.graphics.Color(0xFF06B6D4), packShape)
                    }
                    activeIconPack.contains("pastel") -> {
                        val pastelColors = listOf(
                            androidx.compose.ui.graphics.Color(0xFFFDE2E4),
                            androidx.compose.ui.graphics.Color(0xFFFFCAD4),
                            androidx.compose.ui.graphics.Color(0xFFB5E2FA),
                            androidx.compose.ui.graphics.Color(0xFFE2F0D9),
                            androidx.compose.ui.graphics.Color(0xFFE8F0FE),
                            androidx.compose.ui.graphics.Color(0xFFF0E6FF)
                        )
                        val idx = Math.abs(packageName.hashCode()) % pastelColors.size
                        androidx.compose.ui.Modifier.background(pastelColors[idx])
                    }
                    activeIconPack.contains("monochrome") || activeIconPack.contains("wireframe") -> {
                        androidx.compose.ui.Modifier
                            .background(androidx.compose.ui.graphics.Color(0xFF09090B))
                            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), packShape)
                    }
                    activeIconPack.contains("pixel") -> {
                        androidx.compose.ui.Modifier
                            .background(androidx.compose.ui.graphics.Color(0xFFEF4444))
                            .border(2.dp, androidx.compose.ui.graphics.Color.Black, packShape)
                    }
                    activeIconPack.contains("gold") -> {
                        androidx.compose.ui.Modifier.background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(androidx.compose.ui.graphics.Color(0xFFFCD34D), androidx.compose.ui.graphics.Color(0xFFD97706))
                            )
                        )
                    }
                    activeIconPack.contains("vintage") -> {
                        androidx.compose.ui.Modifier
                            .background(androidx.compose.ui.graphics.Color(0xFFF5E6CA))
                            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFD4B499), packShape)
                    }
                    activeIconPack.contains("material") -> {
                        androidx.compose.ui.Modifier
                            .background(com.piuu.launcher.ui.theme.activePrimaryColor.copy(alpha = 0.2f))
                            .border(1.dp, com.piuu.launcher.ui.theme.activePrimaryColor, packShape)
                    }
                    else -> androidx.compose.ui.Modifier
                }
            )

        androidx.compose.foundation.layout.Box(
            modifier = packModifier,
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            val tint = when {
                activeIconPack.contains("neon") -> androidx.compose.ui.graphics.Color(0xFF06B6D4)
                activeIconPack.contains("pastel") -> androidx.compose.ui.graphics.Color(0xFF475569)
                activeIconPack.contains("monochrome") || activeIconPack.contains("wireframe") -> androidx.compose.ui.graphics.Color.White
                activeIconPack.contains("pixel") -> androidx.compose.ui.graphics.Color.Black
                activeIconPack.contains("gold") -> androidx.compose.ui.graphics.Color(0xFF451A03)
                activeIconPack.contains("vintage") -> androidx.compose.ui.graphics.Color(0xFF5C3D2E)
                activeIconPack.contains("material") -> com.piuu.launcher.ui.theme.activePrimaryColor
                else -> tintColor
            }

            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = androidx.compose.ui.Modifier
                        .size(24.dp)
                        .clip(packShape),
                    colorFilter = if (tint != null) androidx.compose.ui.graphics.ColorFilter.tint(tint) else null
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = when(fallbackIconName) {
                        "phone" -> androidx.compose.material.icons.Icons.Default.Phone
                        "message" -> androidx.compose.material.icons.Icons.Default.Message
                        "globe" -> androidx.compose.material.icons.Icons.Default.Public
                        "camera" -> androidx.compose.material.icons.Icons.Default.CameraAlt
                        "brain" -> androidx.compose.material.icons.Icons.Default.AutoAwesome
                        "folder" -> androidx.compose.material.icons.Icons.Default.Folder
                        "code" -> androidx.compose.material.icons.Icons.Default.Code
                        "music" -> androidx.compose.material.icons.Icons.Default.MusicNote
                        "mail" -> androidx.compose.material.icons.Icons.Default.Mail
                        "video" -> androidx.compose.material.icons.Icons.Default.PlayCircle
                        "calendar" -> androidx.compose.material.icons.Icons.Default.CalendarToday
                        "settings" -> androidx.compose.material.icons.Icons.Default.Settings
                        "note" -> androidx.compose.material.icons.Icons.Default.EditNote
                        "calculator" -> androidx.compose.material.icons.Icons.Default.Calculate
                        "cloud" -> androidx.compose.material.icons.Icons.Default.Cloud
                        "file" -> androidx.compose.material.icons.Icons.Default.InsertDriveFile
                        "clock" -> androidx.compose.material.icons.Icons.Default.AccessTime
                        else -> androidx.compose.material.icons.Icons.Default.Apps
                    },
                    contentDescription = null,
                    tint = tint ?: androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = androidx.compose.ui.Modifier.size(24.dp)
                )
            }
        }
    } else {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier.clip(chosenShape)
            )
        } else {
            androidx.compose.material3.Icon(
                imageVector = when(fallbackIconName) {
                    "phone" -> androidx.compose.material.icons.Icons.Default.Phone
                    "message" -> androidx.compose.material.icons.Icons.Default.Message
                    "globe" -> androidx.compose.material.icons.Icons.Default.Public
                    "camera" -> androidx.compose.material.icons.Icons.Default.CameraAlt
                    "brain" -> androidx.compose.material.icons.Icons.Default.AutoAwesome
                    "folder" -> androidx.compose.material.icons.Icons.Default.Folder
                    "code" -> androidx.compose.material.icons.Icons.Default.Code
                    "music" -> androidx.compose.material.icons.Icons.Default.MusicNote
                    "mail" -> androidx.compose.material.icons.Icons.Default.Mail
                    "video" -> androidx.compose.material.icons.Icons.Default.PlayCircle
                    "calendar" -> androidx.compose.material.icons.Icons.Default.CalendarToday
                    "settings" -> androidx.compose.material.icons.Icons.Default.Settings
                    "note" -> androidx.compose.material.icons.Icons.Default.EditNote
                    "calculator" -> androidx.compose.material.icons.Icons.Default.Calculate
                    "cloud" -> androidx.compose.material.icons.Icons.Default.Cloud
                    "file" -> androidx.compose.material.icons.Icons.Default.InsertDriveFile
                    "clock" -> androidx.compose.material.icons.Icons.Default.AccessTime
                    else -> androidx.compose.material.icons.Icons.Default.Apps
                },
                contentDescription = null,
                tint = tintColor ?: androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    fallbackIconName: String = "apps",
    tintColor: androidx.compose.ui.graphics.Color? = null
) {
    AppIcon(
        packageName = packageName,
        modifier = modifier,
        fallbackIconName = fallbackIconName,
        tintColor = tintColor
    )
}


