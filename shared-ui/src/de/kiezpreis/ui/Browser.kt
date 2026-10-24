package de.kiezpreis.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kiezpreis.core.model.Retailer

/**
 * State of the embedded retailer browser, shared between the screen around it and the platform's
 * WebView. Requests are counters: the WebView reacts whenever one changes.
 */
class BrowserState(val retailer: Retailer, val startUrl: String, val onCaptured: (String) -> Unit) {
    var title by mutableStateOf<String?>(null)
    var currentUrl by mutableStateOf(startUrl)
    var progress by mutableStateOf(0f)
    var loading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)
    var capturing by mutableStateOf(false)
    /** Set when a link left the retailer's site and was therefore not opened. */
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
