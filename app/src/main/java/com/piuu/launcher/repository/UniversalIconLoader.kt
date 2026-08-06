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
import androidx.compose.ui.graphics.ImageBitmap
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

class UniversalIconLoader private constructor(private val context: Context) {
    private val preferenceManager = LauncherPreferenceManager.getInstance(context)
    private val iconPackManager = IconPackManager(context)
    private val pm = context.packageManager

    // Fast in-memory ImageBitmap cache (holds up to 250 decoded icons for 60fps scrolling)
    private val imageBitmapCache = LruCache<String, ImageBitmap>(250)

    // Base64 WebP memory cache
    private val base64Cache = LruCache<String, String>(150)

    // Disk cache directory
    private val diskCacheDir = File(context.cacheDir, "icon_disk_cache").apply {
        if (!exists()) mkdirs()
    }

    companion object {
        @Volatile
        private var instance: UniversalIconLoader? = null

        fun getInstance(context: Context): UniversalIconLoader {
            return instance ?: synchronized(this) {
                instance ?: UniversalIconLoader(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Synchronous fast-path memory cache lookup for 60fps Compose scrolling.
     */
    fun getCachedBitmap(packageName: String): ImageBitmap? {
        return imageBitmapCache.get(packageName)
    }

    suspend fun loadIconBitmap(packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        imageBitmapCache.get(packageName)?.let { return@withContext it }

        val bitmap = resolveBitmap(packageName, null) ?: return@withContext null
        val imageBitmap = bitmap.asImageBitmap()
        imageBitmapCache.put(packageName, imageBitmap)
        return@withContext imageBitmap
    }

    private fun getActiveIconPackAndVersion(): Pair<String, String> {
        try {
            val repository = LauncherRepository(context)
            val schema = repository.getSchema()
            val activeIconPack = schema.active_icon_pack
            if (activeIconPack.isEmpty() || activeIconPack.equals("default", ignoreCase = true)) {
                return Pair("default", "1.0.0")
            }

            val marketplaceCore = MarketplaceCore.getInstance(context)
            val installedPlugins = marketplaceCore.installedPluginsFlow.value
            val plugin = installedPlugins.find { it.manifest.id.equals(activeIconPack, ignoreCase = true) }
            if (plugin != null) {
                return Pair(activeIconPack, plugin.manifest.version)
            }

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

    private fun getDiskCachedIcon(packageName: String, activityName: String?, currentPack: String, currentVersion: String): String? {
        val sanitizedKey = "${packageName.replace(".", "_")}_${activityName?.replace(".", "_") ?: "main"}"
        val cacheFile = File(diskCacheDir, "${sanitizedKey}.cache")
        if (!cacheFile.exists()) return null

        try {
            cacheFile.bufferedReader().use { reader ->
                val cachedHeader = reader.readLine() ?: return null
                val expectedHeader = "v1:${currentPack}:${currentVersion}"
                if (cachedHeader == expectedHeader) {
                    return reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e("UniversalIconLoader", "Failed to read disk cache for $packageName", e)
        }
        return null
    }

    private fun saveDiskCachedIcon(packageName: String, activityName: String?, currentPack: String, currentVersion: String, base64Str: String) {
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
        base64Cache.get(cacheKey)?.let { return@withContext it }

        val (currentPack, currentVersion) = getActiveIconPackAndVersion()
        getDiskCachedIcon(packageName, activityName, currentPack, currentVersion)?.let { cachedBase64 ->
            base64Cache.put(cacheKey, cachedBase64)
            return@withContext cachedBase64
        }

        val iconBitmap = resolveBitmap(packageName, activityName) ?: throw Exception("Icon not found for $packageName")
        val scaled = Bitmap.createScaledBitmap(iconBitmap, 128, 128, true)
        
        // Cache ImageBitmap directly for Compose
        imageBitmapCache.put(packageName, scaled.asImageBitmap())

        val outputStream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, outputStream)
        } else {
            @Suppress("DEPRECATION")
            scaled.compress(Bitmap.CompressFormat.WEBP, 80, outputStream)
        }
        val byteArray = outputStream.toByteArray()

        val base64Str = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        base64Cache.put(cacheKey, base64Str)
        saveDiskCachedIcon(packageName, activityName, currentPack, currentVersion, base64Str)
        base64Str
    }

    private fun resolveBitmap(packageName: String, activityName: String?): Bitmap? {
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

        try {
            val customDir = File(context.filesDir, "custom_icons")
            val customFile = File(customDir, "$packageName.webp")
            if (customFile.exists()) {
                val localBitmap = BitmapFactory.decodeFile(customFile.absolutePath)
                if (localBitmap != null) {
                    return localBitmap
                }
            }
        } catch (e: Exception) {}

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
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    fallbackIconName: String = "apps",
    tintColor: androidx.compose.ui.graphics.Color? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val iconLoader = remember { UniversalIconLoader.getInstance(context) }
    val prefManager = remember { LauncherPreferenceManager.getInstance(context) }
    
    val chosenShape = remember(prefManager.adaptiveIconShape) {
        when (prefManager.adaptiveIconShape.uppercase()) {
            "CIRCLE" -> androidx.compose.foundation.shape.CircleShape
            "SQUIRCLE" -> androidx.compose.foundation.shape.RoundedCornerShape(24)
            "TEARDROP" -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            "ROUNDED_RECT" -> androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            else -> androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        }
    }

    // Fast-path memory cache lookup
    val cachedBitmap = remember(packageName) { iconLoader.getCachedBitmap(packageName) }
    
    val iconBitmapState = androidx.compose.runtime.produceState<ImageBitmap?>(
        initialValue = cachedBitmap, 
        key1 = packageName, 
        key2 = iconBase64
    ) {
        if (cachedBitmap != null) {
            value = cachedBitmap
        } else {
            val loaded = iconLoader.loadIconBitmap(packageName)
            if (loaded != null) {
                value = loaded
            } else if (!iconBase64.isNullOrEmpty()) {
                try {
                    val rawBase64 = if (iconBase64.contains(",")) iconBase64.substringAfter(",") else iconBase64
                    val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    value = bitmap?.asImageBitmap()
                } catch (_: Exception) {}
            }
        }
    }

    val bitmap = iconBitmapState.value

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
            tint = tintColor ?: com.piuu.launcher.ui.theme.PrimaryBlue,
            modifier = modifier
        )
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
