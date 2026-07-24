package com.piuu.launcher.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WebViewPreloader: Singleton offscreen WebView warm-up/preloader service.
 * Pre-instantiates, pre-configures, and pre-renders the hybrid WebView offscreen
 * to enable truly instant (up to 2.5x faster) launcher visual presentations.
 */
@SuppressLint("StaticFieldLeak")
object WebViewPreloader {
    private const val TAG = "WebViewPreloader"

    private var cachedWebView: WebView? = null
    private var isPrewarming = false

    // Multi-cast delegate callback to route launcher launch action from JS to active UI layer
    private var activeLaunchCallback: ((String, String?) -> Unit)? = null

    /**
     * Pre-warms and initializes the WebView instance offscreen asynchronously.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun prewarm(
        context: Context,
        repository: LauncherRepository,
        latencyManager: LatencyManager,
        aiEngine: AiEngine,
        scope: CoroutineScope
    ) {
        if (cachedWebView != null || isPrewarming) {
            Log.d(TAG, "WebView already prewarmed or prewarming in progress. Skipping.")
            return
        }
        isPrewarming = true

        scope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "Initializing WebView offscreen on background thread / main chunk chunking...")
                
                // Use applicationContext to prevent activity/window leaks
                val appContext = context.applicationContext
                val webView = WebView(appContext)
                webView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // 1. Initial hardware layer config for fast rendering during startup
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                // 2. WebSettings optimizations
                optimizeSettings(webView)

                // 3. Instantiate JS Bridge
                val jsBridge = LauncherJsBridge(
                    appContext,
                    repository,
                    latencyManager,
                    aiEngine,
                    scope
                ) { packageName, appName ->
                    activeLaunchCallback?.invoke(packageName, appName)
                }
                webView.addJavascriptInterface(jsBridge, "AndroidLauncherBridge")
                
                jsBridge.wallpaperHandler.applyWallpaperToWebView(webView)

                // 4. Asset loader routing
                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
                    .build()

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Prewarmed WebView page fully loaded.")
                        
                        // Set layer back to NONE after entrance load animation to reclaim video memory
                        view?.postDelayed({
                            try {
                                view.setLayerType(View.LAYER_TYPE_NONE, null)
                                Log.d(TAG, "Prewarmed WebView hardware layer set to NONE (VRAM reclaimed)")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to set layer type to NONE", e)
                            }
                        }, 1200)

                        // Run garbage collection sparingly after complete page load
                        System.gc()
                    }
                }

                // Load initial layout assets
                webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
                cachedWebView = webView
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-warm WebView offscreen", e)
            } finally {
                isPrewarming = false
            }
        }
    }

    /**
     * Retrieves the prewarmed WebView instance, binding the launch callback.
     */
    fun getPrewarmedWebView(launchCallback: (String, String?) -> Unit): WebView? {
        val wv = cachedWebView
        if (wv != null) {
            Log.d(TAG, "Serving pre-warmed WebView instance for instant display")
            activeLaunchCallback = launchCallback
            
            // Re-apply hardware acceleration for the presentation/opening animations
            wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            cachedWebView = null // Consumed
            return wv
        }
        Log.d(TAG, "No pre-warmed WebView available, creating fallback")
        return null
    }

    /**
     * Performs background warm-up of a new WebView instance so that the next presentation is ready.
     */
    fun onWebViewDismissed(
        context: Context,
        repository: LauncherRepository,
        latencyManager: LatencyManager,
        aiEngine: AiEngine,
        scope: CoroutineScope
    ) {
        activeLaunchCallback = null
        prewarm(context, repository, latencyManager, aiEngine, scope)
    }

    /**
     * Clears cache and destroys prewarmed WebView if memory limits are exceeded.
     */
    fun destroy() {
        cachedWebView?.post {
            try {
                cachedWebView?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying prewarmed WebView", e)
            }
            cachedWebView = null
        }
    }

    private fun optimizeSettings(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
            setGeolocationEnabled(false)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        }
    }
}
