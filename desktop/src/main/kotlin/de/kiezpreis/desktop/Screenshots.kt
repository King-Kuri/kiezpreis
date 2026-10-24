package de.kiezpreis.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import de.kiezpreis.core.data.Favorite
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.data.ThemeMode
import de.kiezpreis.core.search.SearchFilters
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.KiezApp
import de.kiezpreis.ui.Tab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.nio.file.Files

/**
 * Renders the real screens headlessly (phone size 412 x 892 dp) in dark and light mode.
 * Data: the built-in, clearly labelled sample data only (no network).
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "build/screenshots").apply { mkdirs() }
    val shots = listOf<Pair<String, (AppController) -> Unit>>(
        "01_home" to { c -> c.selectTab(Tab.HOME) },
        "02_search_monster" to { c -> c.query = "Monster Energy Ultra White"; c.selectTab(Tab.SEARCH) },
        "03_detail_monster" to { c ->
            c.query = "Monster Energy Ultra White"; c.selectTab(Tab.SEARCH)
            val g = c.engine.search(c.query, c.offers.state.value.offers).hits.first().group
            c.openProduct(g)
        },
        "04_search_cola_unitprice" to { c -> c.query = "Cola Zero"; c.sort = de.kiezpreis.core.search.SortOrder.UNIT_PRICE; c.selectTab(Tab.SEARCH) },
        "05_detail_cola" to { c ->
            val g = c.engine.search("Coca Cola Zero", c.offers.state.value.offers).hits.first().group
            c.selectTab(Tab.SEARCH); c.openProduct(g)
        },
        "06_filter_sheet" to { c -> c.query = "Butter"; c.selectTab(Tab.SEARCH); c.filterSheetOpen = true },
        "07_no_match" to { c -> c.query = "Gummibärchen"; c.selectTab(Tab.SEARCH) },
        "08_filtered_away" to { c -> c.query = "Butter"; c.filters = SearchFilters(retailers = setOf(de.kiezpreis.core.model.Retailer.LIDL)); c.selectTab(Tab.SEARCH) },
        "09_favorites" to { c -> c.selectTab(Tab.FAVORITES) },
        "10_settings" to { c -> c.selectTab(Tab.SETTINGS) },
        "11_sources" to { c -> c.selectTab(Tab.SETTINGS); c.sourcesScreenOpen = true },
        "12_search_proteinpulver" to { c -> c.query = "Proteinpulver"; c.selectTab(Tab.SEARCH) },
    )
    for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT)) {
        for ((name, setup) in shots) {
            val dir = Files.createTempDirectory("kiez-shot").toFile()
            val controller = createController(dir, CoroutineScope(Dispatchers.Unconfined))
            controller.user.updatePrefs { it.copy(themeMode = mode, reduceMotion = true, enabledSources = setOf(SourceCatalog.sample.id)) }
            controller.offers.setEnabledSources(setOf(SourceCatalog.sample.id))
            runBlocking { controller.offers.refresh(force = true) }
            seedUserData(controller)
            setup(controller)
            render(controller, File(out, "${name}_${mode.name.lowercase()}.png"))
        }
    }
    // Empty state without any source.
    run {
        val controller = createController(Files.createTempDirectory("kiez-shot").toFile(), CoroutineScope(Dispatchers.Unconfined))
        controller.user.updatePrefs { it.copy(themeMode = ThemeMode.DARK, reduceMotion = true, enabledSources = emptySet()) }
        controller.offers.setEnabledSources(emptySet())
        render(controller, File(out, "13_no_data_dark.png"))
    }
    println("Screenshots written to ${out.absolutePath}")
    kotlin.system.exitProcess(0)
}

private fun seedUserData(c: AppController) {
    val offers = c.offers.state.value.offers
    listOf("Kerrygold", "Monster Ultra White", "Hähnchenbrust").forEach { q ->
        c.engine.search(q, offers).hits.firstOrNull()?.group?.let { g ->
            c.user.toggleFavorite(Favorite(g.key, g.title, g.brand, q, 0))
        }
    }
    c.user.toggleFavorite(Favorite("demo|nicht|vorhanden", "Bio-Hafermilch Barista 1 l", "Beispielmarke", "Hafermilch", 0))
    listOf("Butter", "Proteinpulver", "Coca-Cola Zero").forEach(c.user::addRecentSearch)
}

private fun render(controller: AppController, file: File) {
    val density = Density(2.5f)
    val scene = ImageComposeScene(width = (412 * 2.5).toInt(), height = (892 * 2.5).toInt(), density = density) {
        KiezApp(controller, DesktopPlatform())
    }
    var t = 0L
    repeat(12) { scene.render(t); t += 250_000_000L }
    val image = scene.render(t)
    file.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    scene.close()
}
