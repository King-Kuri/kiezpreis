package de.kiezpreis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.ui.theme.Kiez

/** Selectable chip used for filters and quick actions. Minimum touch height 40 dp, padded to 48 by row spacing. */
@Composable
fun KChip(
    text: String,
    selected: Boolean = false,
    icon: ImageVector? = null,
    trailing: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Kiez.colors
    val bg by animateColorAsState(if (selected) c.accent else c.card, tween(Kiez.motion.short), label = "chipBg")
    val fg = if (selected) c.onAccent else c.textPrimary
    Row(
        modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .background(bg, CircleShape)
            .border(1.dp, if (selected) Color.Transparent else c.cardBorder, CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (trailing != null) Icon(trailing, null, tint = fg.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                color = Kiez.colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

/** Round glass icon button, 48 dp touch target. */
@Composable
fun GlassIconButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, tint: Color = Kiez.colors.textPrimary, onClick: () -> Unit) {
    GlassSurface(modifier = modifier.size(48.dp), shape = CircleShape, elevation = 10.dp, blur = true) {
        Box(
            Modifier.matchParentSize().clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Kiez.colors
    Row(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .background(if (enabled) c.accent else c.divider, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) c.onAccent else c.textTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) c.onAccent else c.textTertiary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Kiez.colors
    Row(
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(CircleShape)
            .background(c.accentSoft, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = if (enabled) c.accent else c.textTertiary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SettingSwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = Kiez.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) c.textPrimary else c.textTertiary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                uncheckedThumbColor = c.textTertiary, uncheckedTrackColor = c.divider, uncheckedBorderColor = c.cardBorder,
            ),
        )
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Kiez.colors.divider))
}
