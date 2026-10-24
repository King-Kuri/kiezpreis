package de.kiezpreis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import de.kiezpreis.core.search.SortOrder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kiezpreis.core.data.DataState
import de.kiezpreis.core.data.SourceStatus
import de.kiezpreis.core.data.UserPrefs
import de.kiezpreis.core.search.SearchFilters
import de.kiezpreis.core.search.SearchResult
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.components.GlassSearchField
import de.kiezpreis.ui.components.KChip
import de.kiezpreis.ui.components.PrimaryButton
import de.kiezpreis.ui.components.ProductResultCard
import de.kiezpreis.ui.components.SampleDataBanner
import de.kiezpreis.ui.components.SecondaryButton
import de.kiezpreis.ui.components.SectionHeader
import de.kiezpreis.ui.components.SourceProblemBanner
import de.kiezpreis.ui.components.StateMessage
import de.kiezpreis.ui.theme.Kiez

/** Height of [SearchTopBar] below the status bar, used as content padding. */
val SearchTopBarHeight = 132.dp

@Composable
fun rememberSearchResult(controller: AppController, data: DataState): SearchResult {
    val q = controller.query
    val f = controller.filters
    val s = controller.sort
    return remember(data.offers, q, f, s) { controller.engine.search(q, data.offers, f, s) }
}

/** Floating glass header: search field plus sort and filter controls. */
@Composable
fun SearchTopBar(controller: AppController, prefs: UserPrefs, modifier: Modifier = Modifier) {
    val f = controller.filters
    Column(modifier.fillMaxWidth()) {
        GlassSearchField(
            value = controller.query,
            onValueChange = { controller.query = it },
            onSubmit = { controller.submitSearch(it) },
            focusRequest = controller.searchFocusRequest,
            blur = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                KChip(
                    if (f.activeCount > 0) "Filter · ${f.activeCount}" else "Filter",
                    selected = f.activeCount > 0, icon = Icons.Rounded.Tune,
                ) { controller.filterSheetOpen = true }
            }
            item { SortChip(controller) }
            item {
                KChip("Berlin bestätigt", selected = f.onlyConfirmedBerlin, icon = Icons.Rounded.Verified) {
                    controller.filters = f.copy(onlyConfirmedBerlin = !f.onlyConfirmedBerlin)
                }
            }
            if (prefs.preferredRetailers.isNotEmpty()) {
                item {
                    val mine = f.retailers == prefs.preferredRetailers
                    KChip("Meine Händler", selected = mine, icon = Icons.Rounded.Star) {
                        controller.filters = f.copy(retailers = if (mine) emptySet() else prefs.preferredRetailers)
                    }
                }
            }
            item {
                KChip("Nur reduziert", selected = f.onlyWithDiscount) { controller.filters = f.copy(onlyWithDiscount = !f.onlyWithDiscount) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchContent(controller: AppController, data: DataState, recent: List<String>, padding: PaddingValues) {
    val c = Kiez.colors
    val today = controller.today()
    val result = rememberSearchResult(controller, data)
    val browsing = controller.query.isBlank() && controller.filters.activeCount == 0
    val suggestions = remember(controller.query, data.offers) {
        val q = de.kiezpreis.core.search.TextNormalizer.normalizeQuery(controller.query)
        controller.engine.suggestions(controller.query, data.offers).filterNot { de.kiezpreis.core.search.TextNormalizer.normalizeQuery(it) == q }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val failed = data.failedSources
        if (failed.isNotEmpty()) {
            item {
                SourceProblemBanner(
                    failed.joinToString(" · ") { "${it.descriptor.name}: ${(it.status as SourceStatus.Failed).reason.label}" } +
                        ". Ergebnisse können unvollständig sein.",
                    Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        if (data.onlySampleData && !browsing) {
            item { SampleDataBanner(Modifier.padding(horizontal = 16.dp)) }
        }
        when {
            !data.hasAnyData -> item {
                when {
                    data.isRefreshing -> StateMessage(Icons.Rounded.Sync, "Lade Angebote …", "Einen Moment, die Quellen werden abgefragt.")
                    failed.isNotEmpty() -> StateMessage(
                        Icons.Rounded.CloudOff, "Datenquelle nicht erreichbar",
                        "Es gibt noch keine gespeicherten Preise. Sobald die Quelle wieder antwortet, erscheinen hier Ergebnisse.",
                        tint = c.caution,
                    ) { PrimaryButton("Erneut versuchen", icon = Icons.Rounded.Sync) { controller.refresh(force = true) } }
                    else -> StateMessage(Icons.Rounded.Inbox, "Keine Daten vorhanden", "Es ist keine Datenquelle aktiv. Aktiviere eine Quelle in den Einstellungen.")
                }
            }
            controller.query.isNotBlank() && suggestions.isNotEmpty() && result.hits.isEmpty() && result.unfilteredMatchCount == 0 -> item {
                Column {
                    StateMessage(
                        Icons.Rounded.SearchOff, "Kein passendes Angebot",
                        "Für „${controller.query}“ gibt es in den aktuellen Berliner Daten keinen Treffer. Meintest du:",
                    )
                    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { s -> KChip(s) { controller.submitSearch(s) } }
                    }
                }
            }
            result.hits.isEmpty() && result.unfilteredMatchCount > 0 -> item {
                StateMessage(
                    Icons.Rounded.FilterAltOff, "Alles weggefiltert",
                    "${Labels.count(result.unfilteredMatchCount, "Preis passt", "Preise passen")} zur Suche, aber nicht zu deinen Filtern.",
                ) { SecondaryButton("Filter zurücksetzen") { controller.filters = SearchFilters() } }
            }
            result.hits.isEmpty() -> item {
                StateMessage(
                    Icons.Rounded.SearchOff, "Kein passendes Angebot",
                    "Für „${controller.query}“ wurde in den aktuellen Berliner Daten nichts gefunden. Das heißt nicht, dass es das Produkt nirgends gibt, sondern nur, dass keine aktive Quelle einen Preis dafür kennt.",
                )
            }
            else -> {}
        }

        if (controller.query.isEmpty() && recent.isNotEmpty() && controller.filters.activeCount == 0) {
            item { SectionHeader("Zuletzt gesucht", action = "Löschen") { controller.user.clearRecentSearches() } }
            item {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { q -> KChip(q, icon = Icons.Rounded.History) { controller.submitSearch(q) } }
                }
            }
        } else if (controller.query.length >= 2 && suggestions.isNotEmpty() && result.hits.isNotEmpty()) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { s -> KChip(s) { controller.submitSearch(s) } }
                }
            }
        }

        if (result.hits.isNotEmpty()) {
            item {
                val offers = result.hits.sumOf { it.group.offers.size }
                Text(
                    (if (browsing) "Alle aktuellen Preise" else Labels.count(result.hits.size, "Produkt", "Produkte")) +
                        " · " + Labels.count(offers, "Preis", "Preise") + " · sortiert nach " + controller.sort.label,
                    style = MaterialTheme.typography.labelMedium, color = c.textTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            items(result.hits, key = { it.group.key }) { hit ->
                ProductResultCard(hit.group, today, hit.quality, Modifier.padding(horizontal = 16.dp)) {
                    if (controller.query.isNotBlank()) controller.user.addRecentSearch(controller.query)
                    controller.openProduct(hit.group)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SortChip(controller: AppController) {
    var open by remember { mutableStateOf(false) }
    val c = Kiez.colors
    Box {
        KChip("Sortierung: " + controller.sort.label, trailing = Icons.Rounded.KeyboardArrowDown) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(c.glassSolid)) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label, color = if (order == controller.sort) c.accent else c.textPrimary) },
                    onClick = { controller.sort = order; open = false },
                )
            }
        }
    }
}
