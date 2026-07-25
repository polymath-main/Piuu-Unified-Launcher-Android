package com.piuu.launcher.ui.components

import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

@Composable
fun LauncherWebView(
    url: String,
    onLayoutJsonReceived: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Apply isolated hardware constraints for peak performance
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = false
                    allowContentAccess = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                }

                // Add secure WebMessageListener if supported by the system WebView
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                    try {
                        WebViewCompat.addWebMessageListener(
                            this,
                            "launcherBridge",
                            setOf("https://appassets.androidplatform.net"),
                            object : WebViewCompat.WebMessageListener {
                                override fun onPostMessage(
                                    view: WebView,
                                    message: WebMessageCompat,
                                    sourceOrigin: Uri,
                                    isMainFrame: Boolean,
                                    replyProxy: JavaScriptReplyProxy
                                ) {
                                    val data = message.data
                                    if (data != null) {
                                        onLayoutJsonReceived(data)
                                    }
                                }
                            }
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }
                }

                loadUrl(url)
            }
        },
        update = { webView ->
            // Update URL or handle live re-loading if necessary
        },
        modifier = modifier.fillMaxSize()
    )
}
