package com.piuu.launcher.repository

import android.app.NotificationManager
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutQuery
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * HardwareControlBridge: Lightweight, battery-optimized bridge connecting
 * Android Framework Hardware APIs and system services to Piuu Launcher.
 */
class HardwareControlBridge private constructor(private val context: Context) {

    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager }

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private var torchCameraId: String? = null

    companion object {
        private const val TAG = "HardwareControlBridge"

        @Volatile
        private var instance: HardwareControlBridge? = null

        fun getInstance(context: Context): HardwareControlBridge {
            return instance ?: synchronized(this) {
                instance ?: HardwareControlBridge(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        setupFlashlightListener()
    }

    // ── 1. Flashlight / Torch Control ──────────────────────────────────────────

    private fun setupFlashlightListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraManager != null) {
            try {
                cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        super.onTorchModeChanged(cameraId, enabled)
                        if (cameraId == torchCameraId || torchCameraId == null) {
                            torchCameraId = cameraId
                            _isFlashlightOn.value = enabled
                        }
                    }

                    override fun onTorchModeUnavailable(cameraId: String) {
                        super.onTorchModeUnavailable(cameraId)
                        if (cameraId == torchCameraId) {
                            _isFlashlightOn.value = false
                        }
                    }
                }, null)
            } catch (e: Exception) {
                Log.w(TAG, "Could not register torch callback: ${e.message}")
            }
        }
    }

    fun toggleFlashlight(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraManager != null) {
            try {
                val camId = torchCameraId ?: getBackCameraWithFlash()
                if (camId != null) {
                    val newState = !_isFlashlightOn.value
                    cameraManager?.setTorchMode(camId, newState)
                    _isFlashlightOn.value = newState
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle flashlight", e)
                Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    private fun getBackCameraWithFlash(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraManager != null) {
            try {
                val cameraIds = cameraManager?.cameraIdList ?: return null
                for (id in cameraIds) {
                    val chars = cameraManager?.getCameraCharacteristics(id)
                    val flashAvailable = chars?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = chars?.get(CameraCharacteristics.LENS_FACING)
                    if (flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        torchCameraId = id
                        return id
                    }
                }
                return cameraIds.firstOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Error finding camera with flash", e)
            }
        }
        return null
    }

    // ── 2. Media & Audio Control ──────────────────────────────────────────────

    fun dispatchMediaKeyEvent(keyCode: Int) {
        audioManager?.let { am ->
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            am.dispatchMediaKeyEvent(eventDown)
            am.dispatchMediaKeyEvent(eventUp)
        }
    }

    fun toggleMediaPlayPause() {
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun skipToNextTrack() {
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun skipToPreviousTrack() {
        dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    // ── 3. System Settings Panels & Intents ───────────────────────────────────

    fun openWifiPanel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback
            }
        }
        openIntentSafely(ctx, Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun openBluetoothPanel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = Intent(Settings.Panel.ACTION_BLUETOOTH).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback
            }
        }
        openIntentSafely(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun openVolumePanel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = Intent(Settings.Panel.ACTION_VOLUME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback
            }
        }
        openIntentSafely(ctx, Intent(Settings.ACTION_SOUND_SETTINGS))
    }

    fun openBatterySaverSettings(ctx: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        } else {
            Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        }
        openIntentSafely(ctx, intent)
    }

    fun toggleDndMode(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
            if (notificationManager!!.isNotificationPolicyAccessGranted) {
                val currentFilter = notificationManager!!.currentInterruptionFilter
                val newFilter = if (currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager!!.setInterruptionFilter(newFilter)
                return newFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                openIntentSafely(ctx, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                Toast.makeText(ctx, "Please grant Do Not Disturb permission", Toast.LENGTH_LONG).show()
                return false
            }
        }
        return false
    }

    fun openDefaultLauncherSelector(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = ctx.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    openIntentSafely(ctx, intent)
                    return
                } else {
                    Toast.makeText(ctx, "Piuu is already your default launcher!", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        openIntentSafely(ctx, Intent(Settings.ACTION_HOME_SETTINGS))
    }

    fun openWallpaperPicker(ctx: Context) {
        try {
            val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(Intent.createChooser(fallback, "Select Wallpaper"))
            } catch (ex: Exception) {
                Toast.makeText(ctx, "No wallpaper app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openClockApp(ctx: Context) {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            // Try standard clock packages
            val clockPkgs = listOf(
                "com.google.android.deskclock",
                "com.android.deskclock",
                "com.sec.android.app.clockpackage",
                "com.oneplus.deskclock",
                "com.coloros.alarmclock",
                "com.xiaomi.alarmclock"
            )
            var opened = false
            for (pkg in clockPkgs) {
                val launchIntent = ctx.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(launchIntent)
                    opened = true
                    break
                }
            }
            if (!opened) {
                Toast.makeText(ctx, "Clock application not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openWeatherApp(ctx: Context) {
        val weatherPkgs = listOf(
            "com.google.android.apps.weather",
            "com.sec.android.daemonapp",
            "com.coloros.weather2",
            "com.xiaomi.weather2"
        )
        for (pkg in weatherPkgs) {
            val launchIntent = ctx.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(launchIntent)
                return
            }
        }
        // Fallback: Web search for local weather
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(webIntent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Cannot open weather info", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSystemSettings(ctx: Context) {
        openIntentSafely(ctx, Intent(Settings.ACTION_SETTINGS))
    }

    // ── 4. Deep App Shortcuts (API 25+) ───────────────────────────────────────

    data class AppShortcutItem(
        val id: String,
        val shortLabel: String,
        val packageName: String
    )

    fun getAppShortcuts(ctx: Context, packageName: String): List<AppShortcutItem> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
                    val query = ShortcutQuery().apply {
                        setPackage(packageName)
                        setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC or ShortcutQuery.FLAG_MATCH_PINNED or ShortcutQuery.FLAG_MATCH_MANIFEST)
                    }
                    val shortcuts: List<ShortcutInfo>? = launcherApps.getShortcuts(query, Process.myUserHandle())
                    if (!shortcuts.isNullOrEmpty()) {
                        return shortcuts.map {
                            AppShortcutItem(
                                id = it.id,
                                shortLabel = (it.shortLabel ?: it.longLabel ?: it.id).toString(),
                                packageName = packageName
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "No shortcut host permission or error: ${e.message}")
            }
        }
        return emptyList()
    }

    fun startAppShortcut(ctx: Context, packageName: String, shortcutId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null) {
                    launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start shortcut $shortcutId", e)
            }
        }
        return false
    }

    private fun openIntentSafely(ctx: Context, intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "Action not supported on this device", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening intent", e)
        }
    }
}
