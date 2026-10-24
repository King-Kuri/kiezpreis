package de.kiezpreis.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Public
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.data.DataState
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.data.SourceDescriptor
import de.kiezpreis.core.data.SourceReadiness
import de.kiezpreis.core.data.SourceState
import de.kiezpreis.core.data.SourceStatus
import de.kiezpreis.core.data.ThemeMode
import de.kiezpreis.core.data.UserPrefs
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.LocalPlatform
import de.kiezpreis.ui.components.FrostCard
import de.kiezpreis.ui.components.Hairline
import de.kiezpreis.ui.components.KChip
import de.kiezpreis.ui.components.Pill
import de.kiezpreis.ui.components.RetailerMark
import de.kiezpreis.ui.components.SecondaryButton
import de.kiezpreis.ui.components.SettingSwitchRow
import de.kiezpreis.ui.theme.Kiez

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(controller: AppController, prefs: UserPrefs, data: DataState, padding: PaddingValues) {
    val c = Kiez.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Einstellungen", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { heading() })
        }
        item { Group("Region") { RegionSection(controller, prefs) } }
        item {
            Group("Bevorzugte Händler") {
                Text(
                    "Bevorzugte Händler werden auf der Startseite markiert und lassen sich in der Suche mit einem Tipp filtern.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 18.dp),
                )
                FlowRow(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Retailer.supermarkets.forEach { r ->
                        val on = r in prefs.preferredRetailers
                        KChip(r.shortName, selected = on) {
                            controller.user.updatePrefs { p -> p.copy(preferredRetailers = if (on) p.preferredRetailers - r else p.preferredRetailers + r) }
                        }
                    }
                }
            }
        }
        item {
            Group("Datenquellen") {
                val active = data.sources.count { it.enabled }
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { controller.sourcesScreenOpen = true }.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Quellen verwalten und testen", style = MaterialTheme.typography.titleSmall)
                        Text("$active aktiv · ${SourceCatalog.all.size} geprüft", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = c.textTertiary)
                }
            }
        }
        item {
            Group("Darstellung") {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { m ->
                        KChip(m.label, selected = prefs.themeMode == m) { controller.user.updatePrefs { it.copy(themeMode = m) } }
                    }
                }
                Hairline()
                SettingSwitchRow("Bewegung reduzieren", "Kurze Überblendungen statt Animationen.", prefs.reduceMotion) { v ->
                    controller.user.updatePrefs { it.copy(reduceMotion = v) }
                }
                SettingSwitchRow("Transparenz reduzieren", "Glasflächen werden deckend, für bessere Lesbarkeit.", prefs.reduceTransparency) { v ->
                    controller.user.updatePrefs { it.copy(reduceTransparency = v) }
                }
            }
        }
        item {
            Group("Datenschutz") {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Kein Konto, keine Anmeldung.",
                        "Kein Standortzugriff. Die Postleitzahl gibst du selbst ein, sie bleibt auf dem Gerät.",
                        "Merkliste, Suchverlauf und Einstellungen werden nur lokal gespeichert.",
                        "Keine Werbe- oder Analysedienste. Abgefragt werden nur die aktivierten Datenquellen.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
            Text(
                "Kiezpreis ${LocalPlatform.current.versionName} · Preisdaten teils von Open Prices (Open Food Facts), lizenziert unter ODbL 1.0.",
                style = MaterialTheme.typography.bodySmall, color = c.textTertiary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun RegionSection(controller: AppController, prefs: UserPrefs) {
    val c = Kiez.colors
    var input by remember(prefs.postalCode) { mutableStateOf(prefs.postalCode ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Place, null, tint = c.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Berlin", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Pill("einzige Region", c.textSecondary, c.divider)
        }
        Spacer(Modifier.height(12.dp))
        Text("Berliner Postleitzahl (optional)", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).background(c.divider, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (input.isEmpty()) Text("z. B. 10245", color = c.textTertiary, fontSize = 16.sp)
                BasicTextField(
                    value = input,
                    onValueChange = { v -> input = v.filter(Char::isDigit).take(5); error = null },
                    singleLine = true,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { error = save(controller, input) }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Postleitzahl" },
                )
            }
            Spacer(Modifier.width(10.dp))
            SecondaryButton("Speichern") { error = save(controller, input) }
        }
        val message = error
        Spacer(Modifier.height(6.dp))
        Text(
            message ?: "Wird verwendet, sobald eine Quelle filialgenaue Daten nach PLZ liefert. Aktuell filtert Kiezpreis ganz Berlin; GPS wird nie benötigt.",
            style = MaterialTheme.typography.bodySmall,
            color = if (message != null && !message.startsWith("Gespeichert")) c.danger else c.textSecondary,
        )
    }
}

private fun save(controller: AppController, input: String): String =
    if (controller.user.setPostalCode(input)) {
        if (input.isBlank()) "Gespeichert: ganz Berlin." else "Gespeichert: $input."
    } else "„$input“ ist keine Berliner Postleitzahl."

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Kiez.colors.textTertiary, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        FrostCard(Modifier.fillMaxWidth()) { Column { content() } }
    }
}

/** Full list of evaluated sources with honest readiness, live status, toggle and connection test. */
@Composable
fun SourcesContent(controller: AppController, data: DataState, padding: PaddingValues) {
    val c = Kiez.colors
    val today = controller.today()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Datenquellen", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(4.dp))
                Text(
                    "Kiezpreis zeigt nur Daten aus diesen Quellen. Fällt eine aus, bleibt ihr letzter Stand sichtbar und gekennzeichnet; es werden nie Ersatzpreise erfunden.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(if (data.isRefreshing) "Aktualisiere …" else "Alle jetzt aktualisieren", icon = Icons.Rounded.Sync, enabled = !data.isRefreshing) {
                    controller.refresh(force = true)
                }
            }
        }
        item(key = "web_import_card") { RetailerImportCard(controller, today, Modifier.padding(horizontal = 16.dp)) }
        SourceCatalog.all.filter { it.id != SourceCatalog.webImport.id }.forEach { d ->
            item(key = d.id) {
                val state = data.sources.firstOrNull { it.descriptor.id == d.id }
                SourceCard(d, state, controller.testResults[d.id], today, Modifier.padding(horizontal = 16.dp),
                    onToggle = { controller.toggleSource(d.id, it) }, onTest = { controller.testSource(d.id) })
            }
        }
    }
}

@Composable
private fun SourceCard(
    d: SourceDescriptor,
    state: SourceState?,
    testResult: String?,
    today: java.time.LocalDate,
    modifier: Modifier,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
) {
    val c = Kiez.colors
    val implemented = state != null
    val (fg, bg) = when (d.readiness) {
        SourceReadiness.AVAILABLE -> c.deal to c.dealSoft
        SourceReadiness.EXPERIMENTAL -> c.caution to c.cautionSoft
        SourceReadiness.PLANNED -> c.info to c.infoSoft
        SourceReadiness.UNAVAILABLE -> c.textTertiary to c.divider
    }
    FrostCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (d.retailer != null) RetailerMark(d.retailer!!, size = 34.dp) else Box(Modifier.size(34.dp).background(c.accentSoft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(if (d.kind == de.kiezpreis.core.data.SourceKind.SAMPLE) Icons.Rounded.Science else Icons.Rounded.Public, null, tint = c.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(d.readiness.label, fg, bg)
                        Pill(d.kind.label, c.textSecondary, c.divider)
                    }
                }
            }
            Text(d.berlinCoverage, style = MaterialTheme.typography.bodySmall, color = c.textPrimary, modifier = Modifier.padding(horizontal = 18.dp))
            Spacer(Modifier.height(4.dp))
            Text(d.notes, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 18.dp))
            Text("Lizenz: ${d.license}", style = MaterialTheme.typography.bodySmall, color = c.textTertiary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
            if (implemented) {
                val status = when (val s = state!!.status) {
                    SourceStatus.Disabled -> "deaktiviert"
                    SourceStatus.NeverLoaded -> "noch nicht abgerufen"
                    SourceStatus.Loading -> "wird abgerufen …"
                    is SourceStatus.Ok -> "OK · ${Labels.count(s.count, "Preis", "Preise")} · ${Labels.timestamp(s.at, today)}" +
                        if (s.rejected > 0) " · ${s.rejected} verworfen" else ""
                    is SourceStatus.Failed -> "Fehler: ${s.reason.label} (${Labels.timestamp(s.at, today)})" +
                        (state.dataFetchedAt?.let { " · zeigt Stand ${Labels.timestamp(it, today)}" } ?: "")
                }
                Text("Status: $status", style = MaterialTheme.typography.labelMedium, color = if (state.status is SourceStatus.Failed) c.caution else c.textSecondary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
                if (testResult != null) {
                    Text("Test: $testResult", style = MaterialTheme.typography.labelMedium, color = c.info, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
                }
                Hairline(Modifier.padding(top = 6.dp))
                SettingSwitchRow("Verwenden", null, state.enabled, onChange = onToggle)
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { SecondaryButton("Verbindung testen", onClick = onTest) }
            } else {
                Text(
                    if (d.readiness == SourceReadiness.PLANNED) "Noch nicht angebunden." else "Wird nicht angebunden.",
                    style = MaterialTheme.typography.labelMedium, color = c.textTertiary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Suppress("unused")
private fun Modifier.roundClip() = this.clip(RoundedCornerShape(8.dp))
