package de.kiezpreis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.kiezpreis.ui.theme.Kiez

/**
 * Centered state message. The app distinguishes, and never mixes up:
 * no match, no data at all, source unreachable, and matches filtered away.
 */
@Composable
fun StateMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tint: Color = Kiez.colors.accent,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).background(
                Brush.radialGradient(listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.04f))), CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Persistent, calm note that the user is looking at demonstration data. */
@Composable
fun SampleDataBanner(modifier: Modifier = Modifier, onSettings: (() -> Unit)? = null) {
    val c = Kiez.colors
    FrostCard(modifier, shape = RoundedCornerShape(20.dp), highlight = c.accent) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Science, null, tint = c.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Beispieldaten", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Preise und Filialen hier sind erfunden und dienen nur zum Ausprobieren. Echte Preise kommen aus den aktivierten Datenquellen.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (onSettings != null) {
                    Spacer(Modifier.height(6.dp))
                    SecondaryButton("Datenquellen", onClick = onSettings)
                }
            }
        }
    }
}

/** Shown above results when a source failed: data may be incomplete or older. */
@Composable
fun SourceProblemBanner(text: String, modifier: Modifier = Modifier) {
    val c = Kiez.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.cautionSoft, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CloudOff, null, tint = c.caution, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.textPrimary)
    }
}
