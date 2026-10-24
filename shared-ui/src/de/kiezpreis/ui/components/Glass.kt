package de.kiezpreis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kiezpreis.ui.LocalPlatform
import de.kiezpreis.ui.theme.Kiez

/**
 * Floating glass surface: backdrop blur (where available), a faint tint, a light rim that is brighter
 * at the top (light coming from above) and a soft specular sheen. Used for navigation, search and
 * floating controls only; content cards use [FrostCard].
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 18.dp,
    tint: Color = Kiez.colors.glass,
    /**
     * Real backdrop blur. Only for elements drawn above the blurred content layer (navigation bar,
     * top bars, sheets). Glass inside scrolling content uses a translucent tint instead.
     */
    blur: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Kiez.colors
    val glass = Kiez.glass
    val platform = LocalPlatform.current
    val useBlur = blur && glass.useBlur
    val fill = when {
        useBlur -> tint
        glass.reduceTransparency -> c.glassSolid
        blur -> c.glassSolid
        else -> c.card
    }
    with(platform) {
        Box(
            modifier = modifier
                // Shadows only under (nearly) opaque layers; under a translucent fill they would shine through.
                .then(if (blur) Modifier.shadow(elevation, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
                .clip(shape)
                .then(if (useBlur) Modifier.backdropBlur(shape, tint, c.glassSolid, 28.dp) else Modifier)
                .background(fill, shape)
                .specularSheen(c.isDark)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(c.glassBorderTop, c.glassBorderBottom)),
                    shape = shape,
                ),
            content = content,
        )
    }
}

/** A diagonal soft highlight in the upper part, giving the glass some depth. */
fun Modifier.specularSheen(dark: Boolean): Modifier = drawWithContent {
    drawContent()
    val alpha = if (dark) 0.07f else 0.35f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.55f, size.height * 1.1f),
        ),
    )
}

/** Content card: translucent over the aurora background, no blur needed, always readable. */
@Composable
fun FrostCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    highlight: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Kiez.colors
    val reduce = Kiez.glass.reduceTransparency
    val base = if (reduce) c.glassSolid else c.card
    Box(
        modifier = modifier
            .clip(shape)
            .background(base, shape)
            .then(
                if (highlight != null) Modifier.background(
                    Brush.linearGradient(listOf(highlight.copy(alpha = if (c.isDark) 0.20f else 0.12f), Color.Transparent)),
                    shape,
                ) else Modifier,
            )
            .border(
                1.dp,
                if (highlight != null) Brush.verticalGradient(listOf(highlight.copy(alpha = 0.55f), highlight.copy(alpha = 0.08f)))
                else Brush.verticalGradient(listOf(c.cardBorder, c.cardBorder.copy(alpha = c.cardBorder.alpha * 0.4f))),
                shape,
            ),
        content = content,
    )
}
