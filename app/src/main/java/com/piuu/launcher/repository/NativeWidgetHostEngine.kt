package com.piuu.launcher.repository

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class NativeWidgetHostEngine(private val activity: Activity, private val hostId: Int = 1024) {
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(activity, hostId)

    init {
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            Log.e("NativeWidgetHostEngine", "Error starting AppWidgetHost listening", e)
        }
    }

    fun getInstalledWidgetsJson(): String {
        val jsonArray = JSONArray()
        try {
            val providers = appWidgetManager.installedProviders
            for (provider in providers) {
                val obj = JSONObject()
                obj.put("provider", provider.provider.flattenToString())
                obj.put("label", provider.loadLabel(activity.packageManager))
                obj.put("minWidth", provider.minWidth)
                obj.put("minHeight", provider.minHeight)
                obj.put("packageName", provider.provider.packageName)
                jsonArray.put(obj)
            }
        } catch (e: Exception) {
            Log.e("NativeWidgetHostEngine", "Error getting installed widgets", e)
        }
        return jsonArray.toString()
    }

    fun createWidgetView(appWidgetId: Int): AppWidgetHostView? {
        try {
            val providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return null
            val hostView = appWidgetHost.createView(activity, appWidgetId, providerInfo)
            hostView.setAppWidget(appWidgetId, providerInfo)
            return hostView
        } catch (e: Exception) {
            Log.e("NativeWidgetHostEngine", "Error creating widget view for id: $appWidgetId", e)
            return null
        }
    }

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deleteWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            Log.e("NativeWidgetHostEngine", "Error stopping widget host", e)
        }
    }
}
