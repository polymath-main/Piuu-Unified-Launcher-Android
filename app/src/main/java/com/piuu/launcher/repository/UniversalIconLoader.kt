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

        val iconBitmap = resolveBitmap(packageName, activityName)
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

    private fun resolveBitmap(packageName: String, activityName: String?): Bitmap {
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
            Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
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
