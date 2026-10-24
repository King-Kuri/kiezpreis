package de.kiezpreis.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.kiezpreis.ui.KiezApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Interactive desktop preview: ./gradlew :desktop:run */
fun main() {
    val dir = File(System.getProperty("user.home"), ".kiezpreis-preview")
    val controller = createController(dir, CoroutineScope(SupervisorJob() + Dispatchers.Main))
    val platform = DesktopPlatform()
    application {
        Window(onCloseRequest = ::exitApplication, title = "Kiezpreis (Vorschau)", state = rememberWindowState(size = DpSize(412.dp, 880.dp))) {
            KiezApp(controller, platform)
        }
    }
}
