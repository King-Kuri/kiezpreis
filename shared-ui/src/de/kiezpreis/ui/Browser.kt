package de.kiezpreis.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kiezpreis.core.importer.ImportSite

/**
 * State of the embedded browser for a retailer site or offer portal, shared between the screen around it and the platform's
 * WebView. Requests are counters: the WebView reacts whenever one changes.
 */
class BrowserState(val site: ImportSite, val onCaptured: (String) -> Unit) {
    val startUrl: String get() = site.startUrl
    var title by mutableStateOf<String?>(null)
    var currentUrl by mutableStateOf(site.startUrl)
    var progress by mutableStateOf(0f)
    var loading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)
    var capturing by mutableStateOf(false)
    /** Set when a link left the site and was therefore not opened. */
    var blockedHost by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    var captureRequest by mutableStateOf(0)
        private set
    var backRequest by mutableStateOf(0)
        private set
    var reloadRequest by mutableStateOf(0)
        private set

    fun capture() {
        if (capturing) return
        capturing = true
        captureRequest++
    }

    fun goBack() { backRequest++ }
    fun reload() { error = null; reloadRequest++ }
}
