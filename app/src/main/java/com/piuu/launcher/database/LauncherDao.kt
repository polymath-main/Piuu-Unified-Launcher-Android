package com.piuu.launcher.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    // App Usage & Favorites
    @Query("SELECT * FROM app_usage ORDER BY usageCount DESC")
    fun getAllAppUsage(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage ORDER BY usageCount DESC")
    suspend fun getAllAppUsageSync(): List<AppUsageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppUsage(appUsage: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAppUsage(appUsageList: List<AppUsageEntity>)

    // Home Widgets & Layout Positions
    @Query("SELECT * FROM home_widgets WHERE pageIndex = :pageIndex ORDER BY y ASC, x ASC")
    fun getWidgetsForPage(pageIndex: Int): Flow<List<HomeWidgetEntity>>

    @Query("SELECT * FROM home_widgets")
    suspend fun getAllWidgetsSync(): List<HomeWidgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: HomeWidgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWidgets(widgets: List<HomeWidgetEntity>)

    @Query("DELETE FROM home_widgets WHERE elementId = :elementId")
    suspend fun deleteWidgetById(elementId: String)

    @Query("DELETE FROM home_widgets")
    suspend fun clearAllWidgets()

    // Dock Shortcuts
    @Query("SELECT * FROM dock_shortcuts ORDER BY position ASC")
    fun getDockShortcuts(): Flow<List<DockShortcutEntity>>

    @Query("SELECT * FROM dock_shortcuts ORDER BY position ASC")
    suspend fun getDockShortcutsSync(): List<DockShortcutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDockShortcut(dockShortcut: DockShortcutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDockShortcuts(shortcuts: List<DockShortcutEntity>)

    @Query("DELETE FROM dock_shortcuts")
    suspend fun clearDockShortcuts()
}
