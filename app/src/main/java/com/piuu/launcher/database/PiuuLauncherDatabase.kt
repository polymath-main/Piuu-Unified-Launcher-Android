package com.piuu.launcher.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppUsageEntity::class, HomeWidgetEntity::class, DockShortcutEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PiuuLauncherDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao

    companion object {
        @Volatile
        private var INSTANCE: PiuuLauncherDatabase? = null

        fun getInstance(context: Context): PiuuLauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PiuuLauncherDatabase::class.java,
                    "piuu_launcher_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
