package com.piuu.launcher.repository

import android.content.Context
import android.content.SharedPreferences

class LauncherPreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("piuu_launcher_prefs", Context.MODE_PRIVATE)

    var iconPackPackageName: String
        get() = prefs.getString(KEY_ICON_PACK_PACKAGE_NAME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_ICON_PACK_PACKAGE_NAME, value).apply()

    companion object {
        private const val KEY_ICON_PACK_PACKAGE_NAME = "key_icon_pack_package_name"

        @Volatile
        private var INSTANCE: LauncherPreferenceManager? = null

        fun getInstance(context: Context): LauncherPreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LauncherPreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
