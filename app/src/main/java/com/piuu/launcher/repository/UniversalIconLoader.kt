package com.piuu.launcher.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Base64
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class UniversalIconLoader(private val context: Context) {
    private val preferenceManager = LauncherPreferenceManager.getInstance(context)
    private val iconPackManager = IconPackManager(context)
    private val pm = context.packageManager

    // Memory cache for Base64 WebP strings (max 100 entries)
    private val memoryCache = LruCache<String, String>(100)

    suspend fun resolveIcon(packageName: String, activityName: String? = null): String = withContext(Dispatchers.IO) {
        val cacheKey = "$packageName/$activityName"
        memoryCache.get(cacheKey)?.let { return@withContext it }

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
        base64Str
    }

    private fun resolveBitmap(packageName: String, activityName: String?): Bitmap? {
        // Layer 1: Check Icon Pack
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
    
    val iconBitmapState = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, 
        key1 = packageName, 
        key2 = iconBase64
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
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.clip(shape)
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


