package de.kiezpreis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.search.SearchFilters
import de.kiezpreis.core.search.SortOrder
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.components.GlassSurface
import de.kiezpreis.ui.components.KChip
import de.kiezpreis.ui.components.PrimaryButton
import de.kiezpreis.ui.components.SecondaryButton
import de.kiezpreis.ui.components.SettingSwitchRow
import de.kiezpreis.ui.theme.Kiez

/** Bottom sheet with all filters; changes apply immediately so the result count stays visible. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(controller: AppController, visible: Boolean, bottomInset: PaddingValues, matchCount: Int) {
    val c = Kiez.colors
    val m = Kiez.motion
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(m.medium)), exit = fadeOut(tween(m.short))) {
            Box(
                Modifier.fillMaxSize().background(c.scrim)
                    .clickable(remember { MutableInteractionSource() }, indication = null) { controller.filterSheetOpen = false }
                    .semantics { contentDescription = "Filter schließen" },
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(m.medium)) { it } + fadeIn(tween(m.medium)),
            exit = slideOutVertically(tween(m.short)) { it } + fadeOut(tween(m.short)),
        ) {
            val f = controller.filters
            GlassSurface(
                Modifier.fillMaxWidth().heightIn(max = 640.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                blur = true,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottomInset).padding(vertical = 12.dp)) {
                    Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(5.dp).background(c.textTertiary.copy(alpha = 0.5f), CircleShape))
                    Spacer(Modifier.height(12.dp))
                    Text("Filter & Sortierung", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
                    SheetLabel("Sortieren nach")
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SortOrder.entries.forEach { o -> KChip(o.label, selected = controller.sort == o) { controller.sort = o } }
                    }
                    SheetLabel("Händler")
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Retailer.supermarkets.forEach { r ->
                            KChip(r.shortName, selected = r in f.retailers) {
                                controller.filters = f.copy(retailers = if (r in f.retailers) f.retailers - r else f.retailers + r)
                            }
                        }
                    }
                    SheetLabel("Kategorie")
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Category.entries.filter { it != Category.OTHER }.forEach { cat ->
                            KChip(cat.label, selected = cat in f.categories) {
                                controller.filters = f.copy(categories = if (cat in f.categories) f.categories - cat else f.categories + cat)
                            }
                        }
                    }
                    SheetLabel("Höchstpreis")
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null, 100L, 200L, 500L, 1000L).forEach { cents ->
                            val label = cents?.let { "bis " + Cents(it).format() } ?: "egal"
                            KChip(label, selected = f.maxPrice?.value == cents) { controller.filters = f.copy(maxPrice = cents?.let(::Cents)) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val home = controller.user.prefs.value.postalCode
                    SettingSwitchRow(
                        "Nur meine Gegend",
                        if (home == null) "Gib zuerst in den Einstellungen deine Postleitzahl an."
                        else "Preise einzelner Märkte nur aus ${de.kiezpreis.core.region.PostalCodes.areaLabel(home)}. Bundesweite und regionale Angebote bleiben.",
                        f.nearPostalCode != null, enabled = home != null,
                    ) { controller.toggleNearby() }
                    SettingSwitchRow("Nur mit belegtem Rabatt", "Nur wenn die Quelle einen früheren Preis nennt.", f.onlyWithDiscount) {
                        controller.filters = f.copy(onlyWithDiscount = it)
                    }
                    SettingSwitchRow("Preisbeobachtungen zeigen", "Von Nutzern gemeldete Regalpreise (keine Prospektangebote).", f.includeObservations) {
                        controller.filters = f.copy(includeObservations = it)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SecondaryButton("Zurücksetzen") { controller.filters = SearchFilters(); controller.sort = SortOrder.RELEVANCE }
                        PrimaryButton(if (matchCount == 1) "1 Produkt zeigen" else "$matchCount Produkte zeigen", Modifier.weight(1f)) { controller.filterSheetOpen = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = Kiez.colors.textSecondary, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 10.dp))
}

