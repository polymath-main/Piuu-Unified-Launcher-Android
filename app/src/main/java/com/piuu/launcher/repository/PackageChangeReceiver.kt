package com.piuu.launcher.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.piuu.launcher.model.SystemApp

class PackageChangeReceiver : BroadcastReceiver() {

    companion object {
        private val callbacks = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

        fun registerCallback(callback: () -> Unit) {
            callbacks.add(callback)
        }

        fun unregisterCallback(callback: () -> Unit) {
            callbacks.remove(callback)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("PackageChangeReceiver", "Received intent action: $action, data: ${intent.dataString}")

        try {
            val repository = LauncherRepository(context)
            val pm = context.packageManager

            // Query system PackageManager for all launcher activities with Android 16 compatibility
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val currentSavedApps = repository.getApps().associateBy { it.package_name }
            val updatedAppsList = mutableListOf<SystemApp>()

            for (resolveInfo in resolvedApps) {
                val pkgName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()

                val existing = currentSavedApps[pkgName]
                if (existing != null) {
                    updatedAppsList.add(existing.copy(name = label))
                } else {
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val category = if (isSystem) "system" else "user"
                    val colorHex = stringToHexColor(pkgName)
                    updatedAppsList.add(
                        SystemApp(
                            package_name = pkgName,
                            name = label,
                            icon_name = "apps",
                            category = category,
                            launch_intent = "intent://$pkgName",
                            usage_count = 0,
                            badge_count = 0,
                            is_favorite = false,
                            accent_color = colorHex,
                            description = "Installed Application ($pkgName)"
                        )
                    )
                }
            }

            // Retain Piuu suite built-in entries if running in simulated container environment
            for (defaultApp in repository.defaultApps) {
                if (defaultApp.package_name.startsWith("com.piuu.") && updatedAppsList.none { it.package_name == defaultApp.package_name }) {
                    updatedAppsList.add(defaultApp)
                }
            }

            if (updatedAppsList.isNotEmpty()) {
                repository.saveApps(updatedAppsList)
                LatencyManager.getInstance().prewarmAppIntents(context, updatedAppsList.map { it.package_name })
                Log.d("PackageChangeReceiver", "Updated launcher app database with ${updatedAppsList.size} apps.")
            }

            // Fire active callbacks (e.g. WebView dynamic updates)
            callbacks.forEach { callback ->
                try {
                    callback.invoke()
                } catch (e: Exception) {
                    Log.e("PackageChangeReceiver", "Error executing change callback", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PackageChangeReceiver", "Failed to update package database on broadcast", e)
        }
    }

    private fun stringToHexColor(str: String): String {
        val hash = str.hashCode()
        val r = (hash and 0xFF0000) shr 16
        val g = (hash and 0x00FF00) shr 8
        val b = hash and 0x0000FF
        return String.format("#%02X%02X%02X", (r + 100) % 256, (g + 100) % 256, (b + 100) % 256)
    }
}
