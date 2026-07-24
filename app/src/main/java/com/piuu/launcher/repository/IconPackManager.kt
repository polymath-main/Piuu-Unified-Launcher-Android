package com.piuu.launcher.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val name: String
)

class IconPackManager(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val packs = mutableListOf<IconPackInfo>()
        val actions = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme"
        )
        for (action in actions) {
            val intent = Intent(action)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (ri in resolveInfos) {
                val pkgName = ri.activityInfo.packageName
                val appLabel = ri.loadLabel(pm).toString()
                if (packs.none { it.packageName == pkgName }) {
                    packs.add(IconPackInfo(pkgName, appLabel))
                }
            }
        }
        return packs
    }

    private val componentToDrawableMap = mutableMapOf<String, String>()
    private val packageToDrawableMap = mutableMapOf<String, String>()
    private var loadedIconPackPkg: String? = null
    private var iconPackResources: Resources? = null

    fun loadIconPack(packPackageName: String): Boolean {
        if (packPackageName.isEmpty() || packPackageName == "system") {
            clear()
            return false
        }
        if (loadedIconPackPkg == packPackageName && iconPackResources != null) {
            return true
        }
        try {
            val packContext = context.createPackageContext(packPackageName, Context.CONTEXT_IGNORE_SECURITY)
            iconPackResources = packContext.resources
            loadedIconPackPkg = packPackageName
            componentToDrawableMap.clear()
            packageToDrawableMap.clear()

            parseAppFilter(packContext, "appfilter")
            parseAppFilter(packContext, "drawable")
            return true
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to load icon pack: $packPackageName", e)
            clear()
            return false
        }
    }

    private fun clear() {
        loadedIconPackPkg = null
        iconPackResources = null
        componentToDrawableMap.clear()
        packageToDrawableMap.clear()
    }

    private fun parseAppFilter(packContext: Context, xmlName: String) {
        try {
            val resId = packContext.resources.getIdentifier(xmlName, "xml", packContext.packageName)
            if (resId == 0) return
            val parser = packContext.resources.getXml(resId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    val pkg = parser.getAttributeValue(null, "package")

                    if (!drawable.isNullOrEmpty()) {
                        if (!component.isNullOrEmpty()) {
                            componentToDrawableMap[component.lowercase()] = drawable
                        }
                        if (!pkg.isNullOrEmpty()) {
                            packageToDrawableMap[pkg.lowercase()] = drawable
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Error parsing $xmlName in icon pack", e)
        }
    }

    fun loadIconFromPack(packageName: String, activityName: String?): Bitmap? {
        val res = iconPackResources ?: return null
        val packPkg = loadedIconPackPkg ?: return null

        val componentKey = if (!activityName.isNullOrEmpty()) {
            "componentinfo{$packageName/$activityName}".lowercase()
        } else {
            ""
        }

        val drawableName = componentToDrawableMap[componentKey]
            ?: packageToDrawableMap[packageName.lowercase()]
            ?: return null

        try {
            val resId = res.getIdentifier(drawableName, "drawable", packPkg)
            if (resId != 0) {
                val d = res.getDrawable(resId, null)
                return drawableToBitmap(d)
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to load drawable $drawableName from pack $packPkg", e)
        }
        return null
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
