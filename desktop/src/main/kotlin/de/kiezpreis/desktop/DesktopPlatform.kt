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
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import de.kiezpreis.core.importer.WebImportSource
import de.kiezpreis.core.importer.WebImportStore
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.BrowserState
import de.kiezpreis.ui.theme.Kiez
import de.kiezpreis.ui.Platform
import kotlinx.coroutines.CoroutineScope
import java.io.File

/** Desktop preview host. No backdrop blur: glass uses its denser fallback, as on Android < 12. */
class DesktopPlatform(
    private val fakeStatusBar: Dp = 28.dp,
    private val fakeNavBar: Dp = 16.dp,
    /** Screenshots only: lay out the import entry points as on Android. */
    override val supportsRetailerBrowser: Boolean = false,
) : Platform {
    override val blurAvailable = false
    override val versionName = "0.1.0-preview"

    @Composable
    override fun systemBarsPadding() = PaddingValues(top = fakeStatusBar, bottom = fakeNavBar)

    @Composable
    override fun BackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

    override fun Modifier.backdropSource(): Modifier = this

    @Composable
    override fun Modifier.backdropBlur(shape: Shape, tint: Color, fallback: Color, blurRadius: Dp): Modifier = background(fallback, shape)

    @Composable
    override fun RetailerBrowser(state: BrowserState, modifier: Modifier) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Händlerseiten lassen sich nur in der Android-App öffnen.", color = Kiez.colors.textSecondary)
        }
    }

    override fun clearRetailerBrowserData() = Unit
}

fun createController(dataDir: File, scope: CoroutineScope, now: () -> Long = System::currentTimeMillis): AppController {
    val store = JsonFileStore(dataDir)
    val imports = WebImportStore(store)
    val repo = OfferRepository(listOf(SampleOfferSource(), OpenPricesSource(OkHttpClientAdapter()), WebImportSource(imports)), store, clock = now)
    return AppController(repo, UserDataRepository(store), imports, scope, now)
}
