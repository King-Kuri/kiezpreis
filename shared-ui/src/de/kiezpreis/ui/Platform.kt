package de.kiezpreis.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Everything the shared UI needs from the host platform (Android app or desktop preview).
 * Keeps the screens free of Android-only APIs so they can be compiled and screenshotted on the JVM.
 */
interface Platform {
    /** Real backdrop blur available (Android 12+ via Haze). */
    val blurAvailable: Boolean

    /** System bar insets. */
    @Composable
    fun systemBarsPadding(): PaddingValues

    /** Hardware/gesture back. */
    @Composable
    fun BackHandler(enabled: Boolean, onBack: () -> Unit)

    /** Marks the content that glass surfaces blur. */
    fun Modifier.backdropSource(): Modifier

    /** Blurs what lies behind this element; implementations without blur draw [fallback] instead. */
    @Composable
    fun Modifier.backdropBlur(shape: Shape, tint: Color, fallback: Color, blurRadius: Dp): Modifier

    /** Version name for the about section. */
    val versionName: String

    /** Whether retailer pages can be opened in an embedded browser (Android WebView only). */
    val supportsRetailerBrowser: Boolean

    /**
     * Shows the retailer page of [state] and answers its requests (back, reload, capture).
     * Navigation stays on the retailer's own domain.
     */
    @Composable
    fun RetailerBrowser(state: BrowserState, modifier: Modifier)

    /** Deletes cookies and site storage the embedded browser collected. */
    fun clearRetailerBrowserData()
}

val LocalPlatform = staticCompositionLocalOf<Platform> { error("No platform") }
