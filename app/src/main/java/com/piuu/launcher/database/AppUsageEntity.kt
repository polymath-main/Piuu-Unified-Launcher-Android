package com.piuu.launcher.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val usageCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastLaunched: Long = System.currentTimeMillis()
)
