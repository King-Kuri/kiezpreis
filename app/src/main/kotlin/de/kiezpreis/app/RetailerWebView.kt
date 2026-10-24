package de.kiezpreis.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import de.kiezpreis.ui.BrowserState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * The retailer's or portal's own website, shown as is. The user navigates, picks a market and answers any
 * consent or security prompt themselves. Kiezpreis reads the page only when the user taps
 * "Angebote übernehmen", and only top-level pages of that site's domain are opened.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RetailerWebView(state: BrowserState, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webView = remember(state) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

            val recorderAtStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
            if (recorderAtStart) {
                val domain = state.site.domain
                WebViewCompat.addDocumentStartJavaScript(this, CaptureScripts.RECORDER, setOf("https://$domain", "https://*.$domain"))
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    val url = request.url
                    val host = url.host.orEmpty()
                    if (url.scheme == "https" && state.site.allows(host)) return false
                    state.blockedHost = host.ifEmpty { url.scheme }
                    return true
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    state.loading = true
                    state.currentUrl = url
                    state.blockedHost = null
                    state.error = null
                    // Older WebViews without document-start scripts: record from here on, best effort.
                    if (!recorderAtStart) view.evaluateJavascript(CaptureScripts.RECORDER, null)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    state.loading = false
                    state.currentUrl = url
                    state.canGoBack = view.canGoBack()
                    state.title = view.title
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) {
                        state.loading = false
                        state.error = "Seite nicht erreichbar. Bist du online?"
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    state.progress = newProgress / 100f
                }
            }
            loadUrl(state.currentUrl)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Requests are counters; drop(1) ignores the value present when this view was (re)created.
    LaunchedEffect(webView) {
        snapshotFlow { state.backRequest }.drop(1).collect { if (webView.canGoBack()) webView.goBack() }
    }
    LaunchedEffect(webView) {
        snapshotFlow { state.reloadRequest }.drop(1).collect { webView.reload() }
    }
    LaunchedEffect(webView) {
        snapshotFlow { state.captureRequest }.drop(1).collect {
            val pageUrl = webView.url.orEmpty()
            webView.evaluateJavascript(CaptureScripts.COLLECT) { result ->
                scope.launch {
                    val payload = withContext(Dispatchers.Default) { withActualUrl(result, pageUrl) }
                    if (payload == null) {
                        state.capturing = false
                        state.error = "Die Seite konnte nicht gelesen werden."
                    } else {
                        state.onCaptured(payload)
                    }
                }
            }
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
}

private val json = Json { isLenient = true }

/**
 * evaluateJavascript returns the script's string result JSON-encoded. The page URL is replaced by
 * the one the WebView reports, so a script on the page cannot claim to be another site.
 */
private fun withActualUrl(result: String?, pageUrl: String): String? = runCatching {
    if (result == null || result == "null") return null
    val inner = json.decodeFromString<String>(result)
    val obj = json.parseToJsonElement(inner).jsonObject
    JsonObject(obj + ("url" to JsonPrimitive(pageUrl))).toString()
}.getOrNull()

fun clearRetailerWebData() {
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
    WebStorage.getInstance().deleteAllData()
}
