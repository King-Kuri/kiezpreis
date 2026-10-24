package de.kiezpreis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EggAlt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.KebabDining
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Cents
import de.kiezpreis.ui.theme.Kiez
import de.kiezpreis.ui.theme.PriceFeatures

/**
 * The current price: euros large, cents slightly smaller. Missing prices are shown as
 * "Preis fehlt", never as 0 €.
 */
@Composable
fun PriceText(price: Cents?, size: TextUnit = 26.sp, color: Color = Kiez.colors.textPrimary, modifier: Modifier = Modifier) {
    if (price == null) {
        Text("Preis fehlt", color = Kiez.colors.textTertiary, fontSize = (size.value * 0.55f).sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
        return
    }
    val euros = price.value / 100
    val cents = (price.value % 100).toString().padStart(2, '0')
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = size, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)) { append("$euros") }
            withStyle(SpanStyle(fontSize = (size.value * 0.62f).sp, fontWeight = FontWeight.Bold)) { append(",$cents") }
            withStyle(SpanStyle(fontSize = (size.value * 0.55f).sp, fontWeight = FontWeight.SemiBold)) { append(" €") }
        },
        color = color,
        style = TextStyle(fontFeatureSettings = PriceFeatures),
        modifier = modifier.semantics { contentDescription = price.format() },
        maxLines = 1,
    )
}

/** Former price, struck through and visually secondary. */
@Composable
fun StrikePrice(price: Cents, modifier: Modifier = Modifier) {
    Text(
        price.format(),
        color = Kiez.colors.textTertiary,
        fontSize = 12.5.sp,
        textDecoration = TextDecoration.LineThrough,
        style = TextStyle(fontFeatureSettings = PriceFeatures),
        modifier = modifier,
    )
}

fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.DRINKS -> Icons.Rounded.LocalDrink
    Category.DAIRY -> Icons.Rounded.EggAlt
    Category.MEAT -> Icons.Rounded.KebabDining
    Category.FRUIT_VEG -> Icons.Rounded.Eco
    Category.BAKERY -> Icons.Rounded.BakeryDining
    Category.PANTRY -> Icons.Rounded.Inventory2
    Category.SWEETS -> Icons.Rounded.Cookie
    Category.FROZEN -> Icons.Rounded.AcUnit
    Category.HOUSEHOLD -> Icons.Rounded.LocalLaundryService
    Category.CARE -> Icons.Rounded.Spa
    Category.SPORTS_NUTRITION -> Icons.Rounded.FitnessCenter
    Category.OTHER -> Icons.Rounded.Category
}

private fun categoryHue(category: Category): Pair<Color, Color> = when (category) {
    Category.DRINKS -> Color(0xFF5B8CFF) to Color(0xFF42D6E8)
    Category.DAIRY -> Color(0xFFFFD27A) to Color(0xFFFFF0C9)
    Category.MEAT -> Color(0xFFFF7A7A) to Color(0xFFFFB38A)
    Category.FRUIT_VEG -> Color(0xFF3CCB7F) to Color(0xFFB6F07A)
    Category.BAKERY -> Color(0xFFE0A15A) to Color(0xFFF6D29B)
    Category.PANTRY -> Color(0xFFB08968) to Color(0xFFE6CCB2)
    Category.SWEETS -> Color(0xFFFF7EB6) to Color(0xFFFFC3A0)
    Category.FROZEN -> Color(0xFF7CC4FF) to Color(0xFFD6F0FF)
    Category.HOUSEHOLD -> Color(0xFF9A8CFF) to Color(0xFF7CE0FF)
    Category.CARE -> Color(0xFFC79BFF) to Color(0xFFFFC1E3)
    Category.SPORTS_NUTRITION -> Color(0xFFFF9E57) to Color(0xFFFFD66B)
    Category.OTHER -> Color(0xFF9AA6B8) to Color(0xFFD7DEE8)
}

/**
 * Product picture placeholder: a softly lit tile with the category symbol. Real product photos are
 * only shown when a source delivers them.
 */
@Composable
fun ProductThumb(category: Category, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val (a, b) = categoryHue(category)
    val dark = Kiez.colors.isDark
    Box(
        modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(a.copy(alpha = if (dark) 0.34f else 0.30f), b.copy(alpha = if (dark) 0.14f else 0.22f))),
                RoundedCornerShape(size * 0.28f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            categoryIcon(category),
            contentDescription = category.label,
            tint = if (dark) b.copy(alpha = 0.95f) else a.copy(red = a.red * 0.75f, green = a.green * 0.75f, blue = a.blue * 0.75f),
            modifier = Modifier.size(size * 0.46f),
        )
    }
}
