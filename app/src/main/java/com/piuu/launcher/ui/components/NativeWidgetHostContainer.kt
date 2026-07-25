package com.piuu.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.piuu.launcher.ui.theme.CardGlassBg
import com.piuu.launcher.ui.theme.PrimaryBlue
import com.piuu.launcher.ui.theme.TextPrimary
import com.piuu.launcher.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun NativeWidgetHostContainer(
    appWidgetId: Int,
    initialColumns: Int = 4,
    initialRows: Int = 2,
    onResize: (columns: Int, rows: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 1024) }

    var widgetColumns by remember { mutableStateOf(initialColumns) }
    var widgetRows by remember { mutableStateOf(initialRows) }
    var isEditing by remember { mutableStateOf(false) }

    val widgetInfo = remember(appWidgetId) {
        try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (e: Exception) {
            Log.e("NativeWidgetHostContainer", "Error fetching AppWidget info for $appWidgetId", e)
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(CardGlassBg)
            .border(
                width = if (isEditing) 2.dp else 1.dp,
                color = if (isEditing) PrimaryBlue else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (widgetInfo == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "App Widget ($appWidgetId) Not Found",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        try {
                            val hostView = appWidgetHost.createView(ctx, appWidgetId, widgetInfo)
                            hostView.layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            addView(hostView)
                        } catch (e: Exception) {
                            Log.e("NativeWidgetHostContainer", "Error binding AppWidget view", e)
                        }
                    }
                },
                update = { view ->
                    view.requestLayout()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Editing Resizing Handles Layer
        if (isEditing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp))
                    .background(PrimaryBlue)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.x > 50) {
                                    widgetColumns = (widgetColumns + 1).coerceIn(1, 6)
                                    onResize(widgetColumns, widgetRows)
                                } else if (dragAmount.x < -50) {
                                    widgetColumns = (widgetColumns - 1).coerceIn(1, 6)
                                    onResize(widgetColumns, widgetRows)
                                }
                                if (dragAmount.y > 50) {
                                    widgetRows = (widgetRows + 1).coerceIn(1, 4)
                                    onResize(widgetColumns, widgetRows)
                                } else if (dragAmount.y < -50) {
                                    widgetRows = (widgetRows - 1).coerceIn(1, 4)
                                    onResize(widgetColumns, widgetRows)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Resize",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
