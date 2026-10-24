package de.kiezpreis.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kiezpreis.core.data.JsonFileStore
import de.kiezpreis.core.data.OfferRepository
import de.kiezpreis.core.data.OkHttpClientAdapter
import de.kiezpreis.core.data.OpenPricesSource
import de.kiezpreis.core.data.SampleOfferSource
import de.kiezpreis.core.data.UserDataRepository
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.Platform
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** Desktop preview host. No backdrop blur: glass uses its denser fallback, as on Android < 12. */
class DesktopPlatform(private val fakeStatusBar: Dp = 28.dp, private val fakeNavBar: Dp = 16.dp) : Platform {
    override val blurAvailable = false
    override val versionName = "0.1.0-preview"

    @Composable
    override fun systemBarsPadding() = PaddingValues(top = fakeStatusBar, bottom = fakeNavBar)

    @Composable
    override fun BackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

    override fun Modifier.backdropSource(): Modifier = this

    @Composable
    override fun Modifier.backdropBlur(shape: Shape, tint: Color, fallback: Color, blurRadius: Dp): Modifier = background(fallback, shape)
}

fun createController(dataDir: File, scope: CoroutineScope, now: () -> Long = System::currentTimeMillis): AppController {
    val store = JsonFileStore(dataDir)
    val repo = OfferRepository(listOf(SampleOfferSource(), OpenPricesSource(OkHttpClientAdapter())), store, clock = now)
    return AppController(repo, UserDataRepository(store), scope, now)
}
