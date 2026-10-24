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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.data.DataState
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.logic.PriceMath
import de.kiezpreis.core.logic.ValidityRules
import de.kiezpreis.core.logic.ValidityStatus
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PackSize
import de.kiezpreis.core.model.PriceKind
import de.kiezpreis.core.search.ProductGroup
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.DetailRoute
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.components.AvailabilityPill
import de.kiezpreis.ui.components.BestDealPill
import de.kiezpreis.ui.components.DiscountBadge
import de.kiezpreis.ui.components.FrostCard
import de.kiezpreis.ui.components.GlassIconButton
import de.kiezpreis.ui.components.Hairline
import de.kiezpreis.ui.components.Pill
import de.kiezpreis.ui.components.PriceText
import de.kiezpreis.ui.components.ProductThumb
import de.kiezpreis.ui.components.RetailerMark
import de.kiezpreis.ui.components.SampleTag
import de.kiezpreis.ui.components.SecondaryButton
import de.kiezpreis.ui.components.StateMessage
import de.kiezpreis.ui.components.StrikePrice
import de.kiezpreis.ui.theme.Kiez
import java.time.LocalDate

val DetailTopBarHeight = 64.dp

@Composable
fun DetailTopBar(controller: AppController, route: DetailRoute, isFavorite: Boolean, group: ProductGroup?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(Icons.Rounded.ArrowBack, "Zurück") { controller.back() }
        Spacer(Modifier.weight(1f))
        if (group != null || isFavorite) {
            GlassIconButton(
                if (isFavorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                if (isFavorite) "Von Merkliste entfernen" else "Auf die Merkliste",
                tint = if (isFavorite) Kiez.colors.accent else Kiez.colors.textPrimary,
            ) {
                if (group != null) controller.toggleFavorite(group) else controller.user.removeFavorite(route.productKey)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailContent(controller: AppController, route: DetailRoute, data: DataState, padding: PaddingValues) {
    val c = Kiez.colors
    val today = controller.today()
    val group = controller.resolveGroup(route.productKey, data.offers)
    if (group == null) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            StateMessage(
                Icons.Rounded.SearchOff, route.fallbackTitle,
                "Aktuell liegt für dieses Produkt kein Berliner Preis vor. Es bleibt auf deiner Merkliste, solange du es nicht entfernst.",
            )
        }
        return
    }
    val isActive: (Offer) -> Boolean = { ValidityRules.status(it, today) != ValidityStatus.UPCOMING }
    val active = group.offers.filter(isActive)
    val upcoming = group.offers.filterNot(isActive)
    val best = group.cheapestComparable(isActive)
    val bySize: List<Pair<PackSize?, List<Offer>>> = active.groupBy { it.packSize }.toList()
        .sortedBy { (_, list) -> list.mapNotNull { it.price?.value }.minOrNull() ?: Long.MAX_VALUE }
    val lead = best ?: active.firstOrNull() ?: group.offers.first()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ProductThumb(lead.category, size = 96.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    group.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary) }
                    Text(group.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Labels.count(group.offers.size, "Preis", "Preise") + " bei " + Labels.count(group.retailers.size, "Händler", "Händlern") +
                            " · " + lead.category.label,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item { BestOfferCard(best, group, today, Modifier.padding(horizontal = 16.dp)) }
        bySize.forEach { (size, offers) ->
            item {
                Text(
                    if (group.hasMixedPackSizes) "Packung: " + (size?.label() ?: "Größe unbekannt") else "Alle Angebote",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                )
            }
            items(offers.sortedBy { it.price?.value ?: Long.MAX_VALUE }, key = { it.id }) { o ->
                OfferRow(o, today, isBest = o.id == best?.id && group.offers.size > 1, Modifier.padding(horizontal = 16.dp))
            }
        }
        if (upcoming.isNotEmpty()) {
            item { Text("Demnächst", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 8.dp)) }
            items(upcoming, key = { it.id }) { o -> OfferRow(o, today, isBest = false, Modifier.padding(horizontal = 16.dp)) }
        }
        item { PriceAlertTeaser(Modifier.padding(horizontal = 16.dp)) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BestOfferCard(best: Offer?, group: ProductGroup, today: LocalDate, modifier: Modifier) {
    val c = Kiez.colors
    if (best == null) {
        FrostCard(modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Info, null, tint = c.info, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (group.offers.none { it.price != null }) "Die Quelle nennt keinen Preis. Kiezpreis setzt keinen Schätzwert ein."
                    else "Kein eindeutiger Preissieger: Die Packungen unterscheiden sich und nicht für alle ist ein Grundpreis bekannt.",
                    style = MaterialTheme.typography.bodyMedium, color = c.textPrimary,
                )
            }
        }
        return
    }
    val unit = PriceMath.unitPrice(best)
    val discount = PriceMath.discountPercent(best)
    FrostCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), highlight = c.deal) {
        Column(Modifier.padding(18.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BestDealPill(text = if (group.offers.size > 1) "Bester vergleichbarer Preis" else "Einziges Angebot")
                if (best.isSample) SampleTag()
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RetailerMark(best.retailer, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(best.retailer.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(best.sizeText ?: "Größe unbekannt", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    PriceText(best.price, size = 34.sp, color = c.textPrimary)
                    unit?.let { Text(it.format(), style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (group.hasMixedPackSizes) {
                Spacer(Modifier.height(10.dp))
                Text("Verglichen über den Grundpreis, weil sich die Packungsgrößen unterscheiden.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            if (discount != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DiscountBadge(discount)
                    Spacer(Modifier.width(8.dp))
                    Text("gegenüber " + best.referencePrice!!.price.format() + " (" + best.referencePrice!!.kind.label + ")", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(ValidityRules.label(best, today) + " · " + Labels.availabilityShort(best), style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfferRow(offer: Offer, today: LocalDate, isBest: Boolean, modifier: Modifier = Modifier) {
    val c = Kiez.colors
    val uri = LocalUriHandler.current
    val unit = PriceMath.unitPrice(offer)
    val discount = PriceMath.discountPercent(offer)
    FrostCard(modifier.fillMaxWidth(), highlight = if (isBest) c.deal else null) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                RetailerMark(offer.retailer, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(offer.retailer.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(offer.title, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    Text(offer.sizeText ?: "Größe unbekannt", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    PriceText(offer.price, size = 24.sp)
                    if (unit != null) Text(unit.format(), style = MaterialTheme.typography.bodySmall)
                    else if (offer.price != null) Text("Grundpreis unbekannt", fontSize = 11.sp, color = c.textTertiary)
                    offer.referencePrice?.let { ref ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StrikePrice(ref.price)
                            if (discount != null) { Spacer(Modifier.width(6.dp)); Text("−$discount %", color = c.deal, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AvailabilityPill(offer)
                Pill(ValidityRules.label(offer, today), c.textSecondary, c.divider)
                if (offer.kind == PriceKind.OBSERVATION) Pill("Preisbeobachtung", c.info, c.infoSoft)
                offer.conditions.forEach { Pill(it.label, c.textSecondary, c.divider) }
                if (offer.isSample) SampleTag()
            }
            Spacer(Modifier.height(10.dp))
            Text(Labels.availabilityLong(offer), style = MaterialTheme.typography.bodySmall)
            if (offer.stores.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                offer.stores.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Rounded.Place, null, tint = c.info, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            listOfNotNull(s.name, s.street, listOfNotNull(s.postalCode, s.city).joinToString(" ").ifBlank { null }).joinToString(", "),
                            style = MaterialTheme.typography.bodySmall, color = c.textPrimary,
                        )
                    }
                }
            }
            ValidityRules.periodLabel(offer.validity)?.takeIf { offer.kind == PriceKind.OFFER }?.let {
                Spacer(Modifier.height(6.dp))
                Text("Angebotszeitraum: $it", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            Spacer(Modifier.height(12.dp))
            Hairline()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sourceName = SourceCatalog.all.firstOrNull { it.id == offer.sourceId }?.name ?: offer.sourceId
                Text(
                    "Quelle: $sourceName · abgerufen ${Labels.timestamp(offer.fetchedAt, today)}",
                    style = MaterialTheme.typography.labelMedium, color = c.textTertiary, modifier = Modifier.weight(1f),
                )
                val url = offer.sourceUrl
                if (url != null) {
                    Row(
                        Modifier.clip(CircleShape).clickable(role = Role.Button) { runCatching { uri.openUri(url) } }.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Zur Quelle", color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.OpenInNew, null, tint = c.accent, modifier = Modifier.size(15.dp))
                    }
                } else if (offer.isSample) {
                    Text("keine Originalquelle", fontSize = 12.sp, color = c.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun PriceAlertTeaser(modifier: Modifier) {
    val c = Kiez.colors
    FrostCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).background(c.accentSoft, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.NotificationsNone, null, tint = c.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Preisalarm", style = MaterialTheme.typography.titleSmall)
                Text(
                    "In Vorbereitung. Ein Alarm wird erst angeboten, wenn eine Quelle regelmäßig und zuverlässig geprüft werden kann. Simulierte Benachrichtigungen gibt es nicht.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Noch nicht verfügbar", enabled = false) {}
            }
        }
    }
}
