package com.piuu.launcher.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_widgets")
data class HomeWidgetEntity(
    @PrimaryKey val elementId: String,
    val type: String,
    val pageIndex: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val w: Int = 1,
    val h: Int = 1,
    val title: String = "",
    val propsJson: String = "{}"
)
