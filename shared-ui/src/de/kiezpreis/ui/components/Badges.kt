package de.kiezpreis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.theme.Kiez

/** Neutral word mark in the chain's colour. Not the retailer's logo. */
@Composable
fun RetailerMark(retailer: Retailer, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val bg = Color(retailer.brandColor)
    val fg = if (bg.luminance() > 0.5f) Color(0xFF111111) else Color.White
    Box(
        modifier
            .size(size)
            .background(Brush.linearGradient(listOf(bg, bg.copy(alpha = 0.82f))), RoundedCornerShape(size * 0.3f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(size * 0.3f))
            .semantics { contentDescription = retailer.displayName },
        contentAlignment = Alignment.Center,
    ) {
        val text = when (retailer) {
            Retailer.ALDI_NORD -> "ALDI N"
            Retailer.ALDI_SUED -> "ALDI S"
            Retailer.KAUFLAND -> "KL"
            Retailer.NETTO_MD -> "NET"
            Retailer.ROSSMANN -> "RM"
            else -> retailer.shortName.take(5).uppercase()
        }
        Text(
            text,
            color = fg,
            fontSize = (size.value * if (text.length > 3) 0.24f else 0.30f).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Small rounded label. */
@Composable
fun Pill(
    text: String,
    color: Color,
    background: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(background, CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(text, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Where an offer applies, at a glance. Calm for chain-wide offers, amber only where the user must take note. */
@Composable
fun AvailabilityPill(offer: Offer, modifier: Modifier = Modifier) {
    val c = Kiez.colors
    val (fg, bg, icon) = when (offer.availability) {
        Coverage.NATIONAL -> Triple(c.textSecondary, c.divider, Icons.Rounded.Public)
        Coverage.REGIONAL -> Triple(c.textSecondary, c.divider, Icons.Rounded.Map)
        Coverage.STORE -> Triple(c.info, c.infoSoft, Icons.Rounded.Place)
        Coverage.UNKNOWN -> Triple(c.caution, c.cautionSoft, Icons.Rounded.HelpOutline)
    }
    Pill(Labels.availabilityShort(offer), fg, bg, icon, modifier)
}

@Composable
fun SampleTag(modifier: Modifier = Modifier) {
    val c = Kiez.colors
    Pill("Beispiel", c.accent, c.accentSoft, Icons.Rounded.Science, modifier)
}

@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    val c = Kiez.colors
    Box(
        modifier
            .background(c.deal, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text("−$percent %", color = c.onDeal, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun BestDealPill(modifier: Modifier = Modifier, text: String = "Günstigstes") {
    val c = Kiez.colors
    Pill(text, c.deal, c.dealSoft, Icons.Rounded.Bolt, modifier)
}
