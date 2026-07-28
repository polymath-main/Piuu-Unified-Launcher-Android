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

    /**
     * Data holder for wallpaper extracted dynamic palette colors.
     */
    data class WallpaperPalette(
        val primaryHex: String = "#3B82F6",
        val vibrantHex: String = "#8B5CF6",
        val accentHex: String = "#38BDF8",
        val bgOverlayHex: String = "rgba(2, 8, 23, 0.85)"
    )

    /**
     * Automatically extracts primary and vibrant color accents from active system wallpaper to harmonize the launcher UI.
     */
    fun extractWallpaperPalette(): WallpaperPalette {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    val p = colors.primaryColor.toArgb()
                    val s = colors.secondaryColor?.toArgb() ?: p
                    val t = colors.tertiaryColor?.toArgb() ?: s

                    fun intToHex(colorInt: Int): String {
                        return String.format("#%06X", 0xFFFFFF and colorInt)
                    }

                    return WallpaperPalette(
                        primaryHex = intToHex(p),
                        vibrantHex = intToHex(s),
                        accentHex = intToHex(t),
                        bgOverlayHex = "rgba(10, 15, 30, 0.85)"
                    )
                }
            }

            // Bitmap Fallback Pixel Sampling
            val bitmap = getWallpaperBitmap()
            if (bitmap != null) {
                val w = bitmap.width
                val h = bitmap.height
                val p1 = bitmap.getPixel((w * 0.3).toInt().coerceIn(0, w - 1), (h * 0.3).toInt().coerceIn(0, h - 1))
                val p2 = bitmap.getPixel((w * 0.7).toInt().coerceIn(0, w - 1), (h * 0.5).toInt().coerceIn(0, h - 1))
                val p3 = bitmap.getPixel((w * 0.5).toInt().coerceIn(0, w - 1), (h * 0.8).toInt().coerceIn(0, h - 1))

                fun intToHex(colorInt: Int): String {
                    return String.format("#%06X", 0xFFFFFF and colorInt)
                }

                return WallpaperPalette(
                    primaryHex = intToHex(p1),
                    vibrantHex = intToHex(p2),
                    accentHex = intToHex(p3),
                    bgOverlayHex = "rgba(10, 15, 30, 0.85)"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting wallpaper palette", e)
        }
        return WallpaperPalette()
    }

    companion object {
        private const val TAG = "WallpaperHandler"
    }
}
