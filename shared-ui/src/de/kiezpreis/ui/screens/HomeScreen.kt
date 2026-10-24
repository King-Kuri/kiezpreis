package de.kiezpreis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.data.DataState
import de.kiezpreis.core.data.Favorite
import de.kiezpreis.core.data.SourceStatus
import de.kiezpreis.core.data.UserPrefs
import de.kiezpreis.core.logic.PriceMath
import de.kiezpreis.core.logic.ValidityRules
import de.kiezpreis.core.logic.ValidityStatus
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.region.CoverageRules
import de.kiezpreis.core.search.Grouping
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.Tab
import de.kiezpreis.ui.components.DealCard
import de.kiezpreis.ui.components.FrostCard
import de.kiezpreis.ui.components.GlassSearchField
import de.kiezpreis.ui.components.KChip
import de.kiezpreis.ui.components.PriceText
import de.kiezpreis.ui.components.PrimaryButton
import de.kiezpreis.ui.components.ProductThumb
import de.kiezpreis.ui.components.RetailerMark
import de.kiezpreis.ui.components.SampleDataBanner
import de.kiezpreis.ui.components.SectionHeader
import de.kiezpreis.ui.components.SourceProblemBanner
import de.kiezpreis.ui.components.StateMessage
import de.kiezpreis.ui.components.categoryIcon
import de.kiezpreis.ui.components.leadOffer
import de.kiezpreis.ui.theme.Kiez

private val homeCategories = listOf(
    Category.DRINKS, Category.DAIRY, Category.MEAT, Category.FRUIT_VEG,
    Category.HOUSEHOLD, Category.SPORTS_NUTRITION, Category.SWEETS, Category.FROZEN,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeContent(
    controller: AppController,
    data: DataState,
    prefs: UserPrefs,
    favorites: List<Favorite>,
    recent: List<String>,
    padding: PaddingValues,
) {
    val c = Kiez.colors
    val today = controller.today()
    val current = remember(data.offers, today) { data.offers.filter { ValidityRules.isCurrent(it, today) } }
    val topDeals = remember(current) {
        Grouping.group(current.filter { CoverageRules.isComparable(it) && ValidityRules.status(it, today) == ValidityStatus.ACTIVE })
            .mapNotNull { g -> leadOffer(g, today).let { o -> PriceMath.discountPercent(o)?.let { g to it } } }
            .sortedByDescending { it.second }
            .take(8)
            .map { it.first }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        buildAnnotatedString {
                            append("Kiez")
                            withStyle(SpanStyle(color = c.accent)) { append("preis") }
                        },
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    StatusLine(data, today, controller)
                }
                RegionChip(prefs.postalCode) { controller.selectTab(Tab.SETTINGS) }
            }
        }
        item {
            GlassSearchField(
                value = "", onValueChange = {}, onSubmit = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = "Was suchst du? z. B. Butter",
                readOnlyClick = { controller.startSearch() },
            )
        }
        val failed = data.failedSources
        if (failed.isNotEmpty()) {
            item {
                SourceProblemBanner(
                    failed.joinToString(" · ") { s -> "${s.descriptor.name}: ${(s.status as SourceStatus.Failed).reason.label}" } +
                        if (data.hasAnyData) ". Angezeigt wird der letzte bekannte Stand." else ".",
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (data.onlySampleData) {
            item { SampleDataBanner(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { controller.selectTab(Tab.SETTINGS); controller.sourcesScreenOpen = true } }
        }
        if (!data.hasAnyData) {
            item {
                if (data.isRefreshing) {
                    StateMessage(Icons.Rounded.Sync, "Lade Angebote …", "Die aktivierten Quellen werden abgefragt.")
                } else if (failed.isNotEmpty()) {
                    StateMessage(Icons.Rounded.CloudOff, "Quellen nicht erreichbar", "Es liegen noch keine gespeicherten Daten vor. Prüfe die Verbindung und versuche es erneut.", tint = c.caution) {
                        PrimaryButton("Erneut versuchen", icon = Icons.Rounded.Sync) { controller.refresh(force = true) }
                    }
                } else {
                    StateMessage(Icons.Rounded.Inbox, "Noch keine Daten", "Aktiviere mindestens eine Datenquelle, um Preise zu sehen.") {
                        PrimaryButton("Datenquellen einrichten") { controller.selectTab(Tab.SETTINGS); controller.sourcesScreenOpen = true }
                    }
                }
            }
        }
        if (topDeals.isNotEmpty()) {
            item { Spacer(Modifier.height(12.dp)); SectionHeader("Starke Angebote", action = "Alle") { controller.query = ""; controller.sort = de.kiezpreis.core.search.SortOrder.DISCOUNT; controller.startSearch(focus = false) } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(topDeals, key = { it.key }) { g -> DealCard(g, today) { controller.openProduct(g) } }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); SectionHeader("Zuletzt gesucht") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recent) { q -> KChip(q, icon = Icons.Rounded.History) { controller.submitSearch(q) } }
                }
            }
        }
        if (data.hasAnyData) item { Spacer(Modifier.height(8.dp)); SectionHeader("Stöbern nach Kategorie") }
        if (data.hasAnyData) item {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2,
            ) {
                homeCategories.forEach { cat ->
                    val count = current.count { it.category == cat }
                    CategoryTile(cat, count, Modifier.weight(1f)) { controller.browseCategory(cat) }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); SectionHeader("Deine Merkliste", action = "Alle") { controller.selectTab(Tab.FAVORITES) } }
            items(favorites.take(3), key = { "fav-" + it.productKey }) { f ->
                val group = controller.resolveGroup(f.productKey, current)
                FavoriteMiniRow(f, group?.let { leadOffer(it, today) }?.price, group?.let { leadOffer(it, today).retailer }) { controller.openFavorite(f) }
            }
        }
        if (data.hasAnyData) item { Spacer(Modifier.height(8.dp)); SectionHeader("Händler") }
        if (data.hasAnyData) item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Retailer.supermarkets) { r ->
                    val count = current.count { it.retailer == r }
                    RetailerTile(r, count, preferred = r in prefs.preferredRetailers) { controller.browseRetailer(r) }
                }
            }
        }
        item {
            Text(
                "Kiezpreis zeigt nur Preise, die eine Quelle belegt. Bei jedem Angebot steht, wo es gilt: bundesweit, in einem Gebiet oder nur in einem Markt.",
                style = MaterialTheme.typography.bodySmall, color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun StatusLine(data: DataState, today: java.time.LocalDate, controller: AppController) {
    val c = Kiez.colors
    val last = data.lastSuccessfulUpdate
    val text = when {
        data.isRefreshing -> "Aktualisiere …"
        data.onlySampleData -> "Beispielmodus"
        last != null -> "Stand " + Labels.timestamp(last, today) + if (data.sources.any { it.enabled && it.isStale }) " (veraltet)" else ""
        else -> "Aktuelle Angebote"
    }
    Text(
        text, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
        modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) { controller.refresh(force = true) }.padding(vertical = 2.dp),
    )
}

@Composable
private fun RegionChip(postalCode: String?, onClick: () -> Unit) {
    val c = Kiez.colors
    Row(
        Modifier.clip(CircleShape).background(c.card, CircleShape).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Place, null, tint = c.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(postalCode ?: "Alle Orte", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary)
    }
}

@Composable
private fun CategoryTile(category: Category, count: Int, modifier: Modifier, onClick: () -> Unit) {
    val c = Kiez.colors
    FrostCard(modifier.clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductThumb(category, size = 42.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(category.label, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(if (count == 0) "keine aktuell" else Labels.count(count, "Preis", "Preise"), fontSize = 12.sp, color = c.textTertiary)
            }
        }
    }
}

@Composable
private fun RetailerTile(retailer: Retailer, count: Int, preferred: Boolean, onClick: () -> Unit) {
    val c = Kiez.colors
    Column(
        Modifier.width(84.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            RetailerMark(retailer, size = 52.dp)
            if (preferred) {
                Box(Modifier.size(12.dp).align(Alignment.TopEnd).background(Brush.radialGradient(listOf(c.deal, c.deal.copy(alpha = 0.6f))), CircleShape))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(retailer.shortName, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary, maxLines = 1)
        Text(if (count == 0) "keine Daten" else "$count", fontSize = 11.5.sp, color = c.textTertiary, maxLines = 1)
    }
}

@Composable
private fun FavoriteMiniRow(f: Favorite, price: de.kiezpreis.core.model.Cents?, retailer: Retailer?, onClick: () -> Unit) {
    val c = Kiez.colors
    FrostCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(if (retailer != null) "günstigst bei ${retailer.shortName}" else "aktuell kein Preis", fontSize = 12.sp, color = c.textTertiary)
            }
            if (price != null) PriceText(price, size = 18.sp)
        }
    }
}
