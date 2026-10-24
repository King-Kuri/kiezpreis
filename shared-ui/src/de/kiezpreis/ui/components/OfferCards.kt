package de.kiezpreis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.logic.PriceMath
import de.kiezpreis.core.logic.ValidityRules
import de.kiezpreis.core.logic.ValidityStatus
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PriceKind
import de.kiezpreis.core.search.MatchQuality
import de.kiezpreis.core.search.ProductGroup
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.theme.Kiez
import java.time.LocalDate

/** Picks the offer a list card leads with: the fair cheapest one, otherwise the lowest price among current offers. */
fun leadOffer(group: ProductGroup, today: LocalDate): Offer {
    val active: (Offer) -> Boolean = { ValidityRules.status(it, today) != ValidityStatus.UPCOMING }
    return group.cheapestComparable(active)
        ?: group.offers.filter(active).filter { it.price != null }.minByOrNull { it.price!!.value }
        ?: group.offers.filter { it.price != null }.minByOrNull { it.price!!.value }
        ?: group.offers.first()
}

/** Search result: one product, its best current price and how many retailers have it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductResultCard(
    group: ProductGroup,
    today: LocalDate,
    quality: MatchQuality,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Kiez.colors
    val lead = leadOffer(group, today)
    val crowned = group.offers.size > 1 && group.cheapestComparable { ValidityRules.status(it, today) != ValidityStatus.UPCOMING } != null
    val discount = PriceMath.discountPercent(lead)
    val unit = PriceMath.unitPrice(lead)
    FrostCard(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            ProductThumb(lead.category, size = 68.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (group.brand != null) {
                    Text(group.brand!!.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1)
                }
                Text(group.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val sizes = group.packSizes.mapNotNull { it?.label() }
                val sizeText = when {
                    sizes.size > 1 -> "${sizes.size} Packungsgrößen"
                    sizes.size == 1 -> sizes.single()
                    else -> lead.sizeText ?: "Größe unbekannt"
                }
                Text(
                    sizeText + " · " + Labels.count(group.retailers.size, "Händler", "Händler"),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PriceText(lead.price, size = 24.sp)
                            if (discount != null) {
                                Spacer(Modifier.width(8.dp))
                                DiscountBadge(discount)
                            }
                        }
                        Text(
                            listOfNotNull(unit?.format(), "bei ${lead.retailer.shortName}").joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, maxLines = 1,
                        )
                    }
                    RetailerMark(lead.retailer, size = 34.dp)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (crowned) BestDealPill()
                    AvailabilityPill(lead)
                    Pill(ValidityRules.label(lead, today), c.textSecondary, c.divider)
                    if (lead.kind == PriceKind.OBSERVATION) Pill("Preisbeobachtung", c.info, c.infoSoft)
                    if (lead.isSample) SampleTag()
                    if (quality == MatchQuality.FUZZY) Pill("ähnlicher Treffer", c.caution, c.cautionSoft)
                }
            }
        }
    }
}

/** Compact highlighted card for the home carousel. */
@Composable
fun DealCard(group: ProductGroup, today: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Kiez.colors
    val lead = leadOffer(group, today)
    val discount = PriceMath.discountPercent(lead)
    FrostCard(
        modifier.width(188.dp).clip(RoundedCornerShape(26.dp)).clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        highlight = if (discount != null && discount >= 25) c.deal else null,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                ProductThumb(lead.category, size = 56.dp)
                Spacer(Modifier.weight(1f))
                RetailerMark(lead.retailer, size = 30.dp)
            }
            Spacer(Modifier.height(12.dp))
            Text(group.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(40.dp))
            Text(lead.sizeText?.removePrefix("je ") ?: " ", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriceText(lead.price, size = 22.sp)
                Spacer(Modifier.weight(1f))
                if (discount != null) DiscountBadge(discount)
            }
            lead.referencePrice?.let {
                Text("statt ${it.price.format()}", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            } ?: Spacer(Modifier.height(17.dp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (lead.isSample) SampleTag() else AvailabilityPill(lead)
            }
            Spacer(Modifier.height(6.dp))
            Text(ValidityRules.label(lead, today), fontSize = 12.sp, color = c.textTertiary, fontWeight = FontWeight.Medium)
        }
    }
}
