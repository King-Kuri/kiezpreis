package de.kiezpreis.app

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import de.kiezpreis.ui.BrowserState
import de.kiezpreis.ui.Platform
import de.kiezpreis.ui.theme.Kiez
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** Android host: real backdrop blur via Haze on Android 12+ (RenderEffect), tinted fallback below. */
class AndroidPlatform(override val versionName: String) : Platform {
    private val haze = HazeState()

    override val blurAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @Composable
    override fun systemBarsPadding(): PaddingValues = WindowInsets.safeDrawing.asPaddingValues()

    @Composable
    override fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
        androidx.activity.compose.BackHandler(enabled, onBack)
    }

    override fun Modifier.backdropSource(): Modifier = hazeSource(haze)

    @Composable
    override fun Modifier.backdropBlur(shape: Shape, tint: Color, fallback: Color, blurRadius: Dp): Modifier {
        val background = Kiez.colors.background
        return hazeEffect(
            state = haze,
            style = HazeStyle(
                backgroundColor = background,
                tints = listOf(HazeTint(tint)),
                blurRadius = blurRadius,
                noiseFactor = 0.06f,
                fallbackTint = HazeTint(fallback),
            ),
        )
    }

    override val supportsRetailerBrowser: Boolean = true

    @Composable
    override fun RetailerBrowser(state: BrowserState, modifier: Modifier) = RetailerWebView(state, modifier)

    override fun clearRetailerBrowserData() = clearRetailerWebData()
}
