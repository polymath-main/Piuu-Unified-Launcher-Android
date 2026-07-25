package com.piuu.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AppWidgetHostEngine(
    appWidgetId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 1024) }
    
    val widgetInfo = remember(appWidgetId) {
        try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (e: Exception) {
            Log.e("AppWidgetHostEngine", "Error loading widget info for $appWidgetId", e)
            null
        }
    }

    if (widgetInfo == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Widget Unavailable ($appWidgetId)", fontSize = 11.sp)
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
                        Log.e("AppWidgetHostEngine", "Error inflating widget", e)
                    }
                }
            },
            update = { view ->
                // Force layout update if size changes
                view.requestLayout()
            },
            modifier = modifier.fillMaxSize()
        )
    }
}
