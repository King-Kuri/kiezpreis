package de.kiezpreis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.ui.Tab
import de.kiezpreis.ui.theme.Kiez

private fun icons(tab: Tab): Pair<ImageVector, ImageVector> = when (tab) {
    Tab.HOME -> Icons.Rounded.Home to Icons.Outlined.Home
    Tab.SEARCH -> Icons.Rounded.Search to Icons.Rounded.Search
    Tab.FAVORITES -> Icons.Rounded.Bookmark to Icons.Rounded.BookmarkBorder
    Tab.SETTINGS -> Icons.Rounded.Tune to Icons.Rounded.Tune
}

/**
 * Floating glass navigation. Inactive tabs are quiet icons; the active tab expands into a softly
 * lit capsule with its label.
 */
@Composable
fun GlassNavBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Kiez.colors
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(34.dp), elevation = 24.dp, blur = true) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val active = tab == current
                val (filled, outline) = icons(tab)
                val bg by animateColorAsState(if (active) c.accentSoft else c.accentSoft.copy(alpha = 0f), tween(Kiez.motion.medium), label = "navBg")
                val fg by animateColorAsState(if (active) c.accent else c.textSecondary, tween(Kiez.motion.medium), label = "navFg")
                Row(
                    Modifier
                        .heightIn(min = 52.dp)
                        .clip(CircleShape)
                        .background(bg, CircleShape)
                        .then(
                            if (active) Modifier.background(
                                Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.18f), c.accent.copy(alpha = 0.0f))), CircleShape,
                            ) else Modifier,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                        ) { onSelect(tab) }
                        .semantics { selected = active; contentDescription = tab.label }
                        .padding(horizontal = if (active) 18.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (active) filled else outline, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
                    AnimatedVisibility(
                        visible = active,
                        enter = fadeIn(tween(Kiez.motion.medium)) + expandHorizontally(tween(Kiez.motion.medium)),
                        exit = fadeOut(tween(Kiez.motion.short)) + shrinkHorizontally(tween(Kiez.motion.short)),
                    ) {
                        Row {
                            Spacer(Modifier.width(8.dp))
                            Text(tab.label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

