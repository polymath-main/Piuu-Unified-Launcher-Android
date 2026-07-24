package com.piuu.launcher.repository

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.WebView
import java.io.ByteArrayOutputStream

/**
 * WallpaperHandler: Native Kotlin handler using WallpaperManager to fetch,
 * encode, and apply system wallpapers for Launcher WebView surfaces and Windows.
 */
class WallpaperHandler(private val context: Context) {

    private val wallpaperManager: WallpaperManager = WallpaperManager.getInstance(context)

    /**
     * Fetches the current system wallpaper as a Drawable.
     */
    fun getWallpaperDrawable(): Drawable? {
        return try {
            wallpaperManager.drawable ?: wallpaperManager.fastDrawable
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException fetching wallpaper drawable", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wallpaper drawable", e)
            null
        }
    }

    /**
     * Converts current system wallpaper Drawable to Bitmap.
     */
    fun getWallpaperBitmap(): Bitmap? {
        val drawable = getWallpaperDrawable() ?: return null
        return try {
            if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting wallpaper to bitmap", e)
            null
        }
    }

    /**
     * Encodes system wallpaper Bitmap to Base64 image data URL string for JavaScript/WebView.
     */
    fun getWallpaperBase64(): String {
        return try {
            var bitmap = getWallpaperBitmap()
            if (bitmap == null) {
                // Generate a stunning fallback gradient bitmap
                bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = android.graphics.Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, 720f, 1280f,
                        intArrayOf(android.graphics.Color.parseColor("#020817"), android.graphics.Color.parseColor("#0f172a"), android.graphics.Color.parseColor("#1e1b4b")),
                        null,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, 720f, 1280f, paint)
            }
            // Scale down if bitmap is huge to optimize payload size for WebView IPC
            val maxDim = 1280
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (aspect > 1f) maxDim else (maxDim * aspect).toInt()
                val targetH = if (aspect > 1f) (maxDim / aspect).toInt() else maxDim
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, stream)
            } else {
                @Suppress("DEPRECATION")
                scaled.compress(Bitmap.CompressFormat.WEBP, 80, stream)
            }
            val bytes = stream.toByteArray()
            "data:image/webp;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating wallpaper base64", e)
            ""
        }
    }

    /**
     * Applies the fetched system wallpaper or transparency background directly to the WebView window/view.
     */
    fun applyWallpaperToWebView(webView: WebView) {
        try {
            webView.post {
                webView.setBackgroundColor(0) // Transparent WebView canvas
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                val drawable = getWallpaperDrawable()
                if (drawable != null) {
                    webView.background = drawable
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying wallpaper to WebView", e)
        }
    }

    /**
     * Sets a new system wallpaper from a Bitmap.
     */
    fun setSystemWallpaper(bitmap: Bitmap): Boolean {
        return try {
            wallpaperManager.setBitmap(bitmap)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting system wallpaper", e)
            false
        }
    }

    companion object {
        private const val TAG = "WallpaperHandler"
    }
}
