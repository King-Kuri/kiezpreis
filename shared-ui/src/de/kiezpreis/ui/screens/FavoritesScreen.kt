package de.kiezpreis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.data.DataState
import de.kiezpreis.core.data.Favorite
import de.kiezpreis.core.logic.PriceMath
import de.kiezpreis.core.logic.ValidityRules
import de.kiezpreis.core.search.TextNormalizer
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.components.AvailabilityPill
import de.kiezpreis.ui.components.FrostCard
import de.kiezpreis.ui.components.GlassSearchField
import de.kiezpreis.ui.components.Pill
import de.kiezpreis.ui.components.PrimaryButton
import de.kiezpreis.ui.components.PriceText
import de.kiezpreis.ui.components.ProductThumb
import de.kiezpreis.ui.components.RetailerMark
import de.kiezpreis.ui.components.SampleTag
import de.kiezpreis.ui.components.StateMessage
import de.kiezpreis.ui.components.leadOffer
import de.kiezpreis.ui.theme.Kiez

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FavoritesContent(controller: AppController, data: DataState, favorites: List<Favorite>, padding: PaddingValues) {
    val c = Kiez.colors
    val today = controller.today()
    var filter by remember { mutableStateOf("") }
    val shown = remember(favorites, filter) {
        val q = TextNormalizer.fold(filter.trim())
        if (q.isEmpty()) favorites else favorites.filter { TextNormalizer.fold(listOfNotNull(it.brand, it.title).joinToString(" ")).contains(q) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Merkliste", style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
                Text(
                    if (favorites.isEmpty()) "Lokal auf diesem Gerät gespeichert, ohne Konto."
                    else Labels.count(favorites.size, "Produkt", "Produkte") + " · lokal gespeichert",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (favorites.isEmpty()) {
            item {
                StateMessage(
                    Icons.Rounded.BookmarkBorder, "Noch nichts gemerkt",
                    "Öffne ein Produkt und tippe auf das Lesezeichen. Hier siehst du dann, wo es in Berlin gerade am günstigsten ist.",
                ) { PrimaryButton("Produkte suchen") { controller.startSearch() } }
            }
            return@LazyColumn
        }
        if (favorites.size > 4) {
            item {
                GlassSearchField(filter, { filter = it }, {}, Modifier.padding(horizontal = 16.dp), placeholder = "Merkliste durchsuchen")
            }
        }
        items(shown, key = { it.productKey }) { f ->
            val group = controller.resolveGroup(f.productKey, data.offers)
            val lead = group?.let { leadOffer(it, today) }
            FrostCard(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button) { controller.openFavorite(f) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (lead != null) ProductThumb(lead.category, size = 56.dp) else ProductThumb(de.kiezpreis.core.model.Category.OTHER, size = 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        f.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary) }
                        Text(f.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Spacer(Modifier.padding(top = 4.dp))
                        if (lead == null) {
                            Pill("Aktuell kein Berliner Preis", c.textSecondary, c.divider)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AvailabilityPill(lead)
                                if (lead.isSample) SampleTag()
                            }
                            Text(
                                ValidityRules.label(lead, today) + " · " + Labels.count(group.retailers.size, "Händler", "Händler"),
                                fontSize = 12.sp, color = c.textTertiary,
                            )
                        }
                    }
                    if (lead != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            RetailerMark(lead.retailer, size = 28.dp)
                            PriceText(lead.price, size = 20.sp)
                            PriceMath.unitPrice(lead)?.let { Text(it.format(), fontSize = 11.sp, color = c.textTertiary) }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Close, null, tint = c.textTertiary,
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button) { controller.user.removeFavorite(f.productKey) }
                            .padding(10.dp).semantics { contentDescription = "${f.title} entfernen" },
                    )
                }
            }
        }
    }
}
