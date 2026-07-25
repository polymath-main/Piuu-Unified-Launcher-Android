package com.piuu.launcher.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "piuu_launcher_config")

class LauncherConfigDataStore(private val context: Context) {

    companion object {
        private val KEY_HOME_ICON_SIZE = intPreferencesKey("home_icon_size")
        private val KEY_DRAWER_ICON_SIZE = intPreferencesKey("drawer_icon_size")
        private val KEY_HOME_COLUMNS = intPreferencesKey("home_columns")
        private val KEY_HOME_ROWS = intPreferencesKey("home_rows")
        private val KEY_DRAWER_COLUMNS = intPreferencesKey("drawer_columns")
        private val KEY_DRAWER_ORIENTATION = stringPreferencesKey("drawer_orientation")
        private val KEY_BACKGROUND_TRANSPARENCY = floatPreferencesKey("background_transparency")
        private val KEY_SHOW_SYSTEM_WALLPAPER = booleanPreferencesKey("show_system_wallpaper")

        @Volatile
        private var INSTANCE: LauncherConfigDataStore? = null

        fun getInstance(context: Context): LauncherConfigDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LauncherConfigDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val configFlow: Flow<LauncherConfig> = context.dataStore.data.map { preferences ->
        LauncherConfig(
            homeIconSize = preferences[KEY_HOME_ICON_SIZE] ?: 56,
            drawerIconSize = preferences[KEY_DRAWER_ICON_SIZE] ?: 52,
            homeColumns = preferences[KEY_HOME_COLUMNS] ?: 4,
            homeRows = preferences[KEY_HOME_ROWS] ?: 6,
            drawerColumns = preferences[KEY_DRAWER_COLUMNS] ?: 4,
            drawerOrientation = preferences[KEY_DRAWER_ORIENTATION] ?: "vertical",
            backgroundTransparency = preferences[KEY_BACKGROUND_TRANSPARENCY] ?: 0.85f,
            showSystemWallpaper = preferences[KEY_SHOW_SYSTEM_WALLPAPER] ?: true
        )
    }

    suspend fun updateHomeIconSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOME_ICON_SIZE] = size
        }
    }

    suspend fun updateDrawerIconSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DRAWER_ICON_SIZE] = size
        }
    }

    suspend fun updateHomeColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOME_COLUMNS] = columns
        }
    }

    suspend fun updateHomeRows(rows: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HOME_ROWS] = rows
        }
    }

    suspend fun updateDrawerColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DRAWER_COLUMNS] = columns
        }
    }

    suspend fun updateDrawerOrientation(orientation: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DRAWER_ORIENTATION] = orientation
        }
    }

    suspend fun updateBackgroundTransparency(transparency: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKGROUND_TRANSPARENCY] = transparency
        }
    }

    suspend fun updateShowSystemWallpaper(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_SYSTEM_WALLPAPER] = show
        }
    }
}
