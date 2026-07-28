package com.piuu.launcher.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dock_shortcuts")
data class DockShortcutEntity(
    @PrimaryKey val packageName: String,
    val position: Int
)
