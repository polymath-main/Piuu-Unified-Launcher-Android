package com.piuu.launcher.ui.components

import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.piuu.launcher.repository.AiEngine
import com.piuu.launcher.repository.LatencyManager
import com.piuu.launcher.repository.LauncherJsBridge
import com.piuu.launcher.repository.LauncherRepository

/**
 * HybridWebViewComposable: Integrates WebView with local WebViewAssetLoader
 * and Native JavaScript bridge, highly optimized for WebKit/Chromium performance.
 * Utilizes prewarmed WebView instances for instantaneous native-grade load latency.
 */
@android.annotation.SuppressLint("JavascriptInterface")
@Composable
fun HybridWebViewComposable(
    repository: LauncherRepository,
    latencyManager: LatencyManager,
    aiEngine: AiEngine,
    onLaunchApp: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val jsBridge = remember {
        LauncherJsBridge(context, repository, latencyManager, aiEngine, scope, onLaunchApp)
    }

    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    // Keep reference to webView to properly dispose of resources and clear memory cache
    var webViewRef: WebView? = null

    AndroidView(
        factory = { ctx ->
            // 1. ATTEMPT TO RETRIEVE PRE-WARMED WEBVIEW
            val prewarmed = com.piuu.launcher.repository.WebViewPreloader.getPrewarmedWebView(onLaunchApp)
            if (prewarmed != null) {
                // Ensure it is detached from any previous virtual layout before adding to Compose
                (prewarmed.parent as? ViewGroup)?.removeView(prewarmed)
                webViewRef = prewarmed
                prewarmed
            } else {
                // 2. FALLBACK ON-DEMAND CREATION IF NOT READY
                WebView(ctx).apply {
                    webViewRef = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Enable Hardware layer for entrance animation
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    optimizeWebViewSettings(this)

                    addJavascriptInterface(jsBridge, "AndroidLauncherBridge")

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            return assetLoader.shouldInterceptRequest(request.url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            
                            // Reclaim hardware layer memory by reverting to LAYER_TYPE_NONE after loading/animation completes
                            view?.postDelayed({
                                try {
                                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                                    android.util.Log.d("HybridWebView", "Hardware layer set to NONE to reclaim video memory")
                                } catch (e: Exception) {
                                    android.util.Log.e("HybridWebView", "Error setting hardware layer to NONE", e)
                                }
                            }, 1200)

                            // Trigger GC sparingly after complete page load
                            System.gc()
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/assets/index.html")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )

    // 5. LIFECYCLE & MEMORY MANAGEMENT COUPLING
    // Ensures clean destruction, RAM reclamation, and broadcast receiver callbacks unregistration when the WebView screen is hidden/recomposed
    DisposableEffect(Unit) {
        val callback = {
            webViewRef?.post {
                webViewRef?.evaluateJavascript("javascript:if(window.onPackageChanged) window.onPackageChanged();", null)
            }
            Unit
        }
        com.piuu.launcher.repository.PackageChangeReceiver.registerCallback(callback)

        onDispose {
            com.piuu.launcher.repository.PackageChangeReceiver.unregisterCallback(callback)
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.clearHistory()
                wv.clearCache(true) // true to clear disk cache, false to clear RAM cache only
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.removeAllViews()
                wv.destroy()
            }
            webViewRef = null
            
            // Re-warm a new WebView instance in the background for the next transition
            com.piuu.launcher.repository.WebViewPreloader.onWebViewDismissed(
                context,
                repository,
                latencyManager,
                aiEngine,
                scope
            )
            
            System.gc() // Actively suggest garbage collection to reclaim Chromium heap space after destruction
        }
    }
}

/**
 * Explicitly configures WebView WebSettings to maximize GPU throughput, 
 * utilize DOM storage APIs, and disable redundant legacy overhead.
 */
private fun optimizeWebViewSettings(webView: WebView) {
    webView.settings.apply {
        // Core Web Capabilities
        javaScriptEnabled = true // Required for our dynamic UI
        domStorageEnabled = true // Enabled for ultra-low latency local cache (localStorage)
        databaseEnabled = true // Enabled for rich local data persistence

        // Network and Caching Pipelines
        // LOAD_DEFAULT optimizes cache validations based on HTTP headers
        cacheMode = WebSettings.LOAD_DEFAULT 

        // Block local file protocol and content access to enforce origin-bound safety (TWA equivalent)
        allowFileAccess = false
        allowContentAccess = false

        // Disable Unused Overhead / Security Surface Area reduction
        setSupportMultipleWindows(false) // Prevents multi-window thread allocation overhead
        setGeolocationEnabled(false) // Blocks geolocation tracking queries
        setSupportZoom(false) // No zoom pinch gestures needed for full-screen launcher
        builtInZoomControls = false // No scaling overlays
        displayZoomControls = false

        // Rendering Engine & Viewport Configuration
        useWideViewPort = true // Configures correct responsive canvas bounds
        loadWithOverviewMode = true // Zoom-to-fit behavior
        loadsImagesAutomatically = true // Instant image decoding
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // Security strictness

        // Optimize layout algorithm and disable text auto-sizing calculation overhead
        layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
    }
}
