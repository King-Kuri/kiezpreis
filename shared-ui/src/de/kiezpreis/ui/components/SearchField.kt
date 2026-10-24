package de.kiezpreis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.ui.theme.Kiez

/** Glass search field. Use [onClick] variant on the home screen (acts as a button). */
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Produkt, Marke oder Händler",
    focusRequest: Int = 0,
    readOnlyClick: (() -> Unit)? = null,
    blur: Boolean = false,
) {
    val c = Kiez.colors
    val focus = remember { FocusRequester() }
    if (readOnlyClick == null) {
        LaunchedEffect(focusRequest) { if (focusRequest > 0) runCatching { focus.requestFocus() } }
    }
    GlassSurface(modifier = modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(29.dp), elevation = 14.dp, blur = blur) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .then(if (readOnlyClick != null) Modifier.clickable(role = Role.Button, onClick = readOnlyClick).semantics { contentDescription = "Suche öffnen" } else Modifier)
                .padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, color = c.textTertiary, fontSize = 16.sp, maxLines = 1)
                }
                if (readOnlyClick == null) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit(value) }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Suchfeld" },
                    )
                }
            }
            if (value.isNotEmpty() && readOnlyClick == null) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) { onValueChange("") }
                        .semantics { contentDescription = "Suche leeren" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
