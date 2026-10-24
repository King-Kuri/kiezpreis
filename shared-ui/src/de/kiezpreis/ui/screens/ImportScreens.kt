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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.importer.ExtractedOffer
import de.kiezpreis.core.importer.ImportDraft
import de.kiezpreis.core.importer.MarketChoice
import de.kiezpreis.core.logic.ValidityRules
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Validity
import de.kiezpreis.core.importer.ImportSites
import de.kiezpreis.core.importer.WebImport
import de.kiezpreis.core.region.PostalCodes
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.BrowserState
import de.kiezpreis.ui.Labels
import de.kiezpreis.ui.LocalPlatform
import de.kiezpreis.ui.components.FrostCard
import de.kiezpreis.ui.components.GlassIconButton
import de.kiezpreis.ui.components.GlassSurface
import de.kiezpreis.ui.components.Hairline
import de.kiezpreis.ui.components.KChip
import de.kiezpreis.ui.components.Pill
import de.kiezpreis.ui.components.PrimaryButton
import de.kiezpreis.ui.components.RetailerMark
import de.kiezpreis.ui.components.SecondaryButton
import de.kiezpreis.ui.components.StateMessage
import de.kiezpreis.ui.components.StrikePrice
import de.kiezpreis.ui.theme.Kiez
import java.time.LocalDate

// ---------------------------------------------------------------- entry card on the sources screen

/** Card on the sources screen: open a retailer page, list and remove earlier imports. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RetailerImportCard(controller: AppController, today: LocalDate, modifier: Modifier = Modifier) {
    val c = Kiez.colors
    val platform = LocalPlatform.current
    val batches by controller.imports.batches.collectAsState()
    val d = SourceCatalog.webImport
    FrostCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).background(c.accentSoft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.OpenInBrowser, null, tint = c.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Angebote von Händlerseiten übernehmen", style = MaterialTheme.typography.titleSmall)
                    Pill(d.readiness.label, c.caution, c.cautionSoft)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(d.notes, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(d.coverage, style = MaterialTheme.typography.bodySmall, color = c.textPrimary)
            Text("Lizenz: ${d.license}", style = MaterialTheme.typography.bodySmall, color = c.textTertiary, modifier = Modifier.padding(top = 4.dp))

            controller.importMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.labelLarge, color = c.deal,
                    modifier = Modifier.fillMaxWidth().background(c.dealSoft, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
            }

            Spacer(Modifier.height(12.dp))
            if (platform.supportsRetailerBrowser) {
                Text("Händlerseite öffnen", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImportSites.retailerSites.forEach { s -> KChip(s.retailer!!.shortName, icon = Icons.Rounded.OpenInBrowser) { controller.openImportSite(s) } }
                }
                Spacer(Modifier.height(12.dp))
                Text("Angebotsportal öffnen (viele Händler auf einer Seite)", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImportSites.portals.forEach { s -> KChip(s.name, icon = Icons.Rounded.OpenInBrowser) { controller.openImportSite(s) } }
                }
                ImportSites.portals.mapNotNull { it.note }.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary, modifier = Modifier.padding(top = 6.dp))
                }
            } else {
                Text("Nur in der Android-App verfügbar.", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            }

            if (batches.isNotEmpty()) {
                Hairline(Modifier.padding(vertical = 12.dp))
                Text("Übernommen", style = MaterialTheme.typography.labelMedium)
                batches.sortedByDescending { it.importedAt }.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val site = ImportSites.byId(b.siteKey)
                        if (b.retailer != null) RetailerMark(b.retailer!!, size = 28.dp)
                        else Icon(Icons.Rounded.OpenInBrowser, null, tint = c.accent, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${site?.name ?: b.siteKey} · ${controller.batchPlace(b)}",
                                style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${Labels.count(b.offers.size, "Preis", "Preise")} · ${Labels.timestamp(b.importedAt, today)}",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        Text("Entfernen", color = c.accent, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable(role = Role.Button) { controller.removeImport(b.siteKey) }.padding(8.dp))
                    }
                }
            }
            if (platform.supportsRetailerBrowser) {
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Cookies der Händlerseiten löschen") { platform.clearRetailerBrowserData() }
            }
        }
    }
}

// ---------------------------------------------------------------- browser

/** Full-screen retailer page with a solid header and the "take over" action below it. */
@Composable
fun BrowserScreen(controller: AppController, state: BrowserState, padding: PaddingValues) {
    val c = Kiez.colors
    val platform = LocalPlatform.current
    Column(Modifier.fillMaxSize().background(c.background).padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Rounded.Close, "Schließen") { controller.closeBrowser() }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(state.site.name, style = MaterialTheme.typography.titleSmall)
                Text(hostOf(state.currentUrl), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GlassIconButton(Icons.Rounded.Refresh, "Neu laden") { state.reload() }
        }
        if (state.loading) LinearProgressIndicator(progress = state.progress, modifier = Modifier.fillMaxWidth().height(2.dp), color = c.accent, trackColor = c.divider)
        else Spacer(Modifier.height(2.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            platform.RetailerBrowser(state, Modifier.fillMaxSize())
        }

        Column(Modifier.fillMaxWidth().background(c.glassSolid).padding(horizontal = 16.dp, vertical = 10.dp)) {
            val note = state.error ?: state.blockedHost?.let { "Link zu $it nicht geöffnet: Kiezpreis bleibt auf ${state.site.domain}." }
            Text(
                note ?: "Wähle auf der Seite deinen Markt und öffne dessen Angebote. Tippe dann auf „Angebote übernehmen“. Du siehst vor dem Speichern, was erkannt wurde.",
                style = MaterialTheme.typography.bodySmall, color = if (note != null) c.caution else c.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                if (state.capturing) "Lese Seite …" else "Angebote übernehmen",
                icon = Icons.Rounded.Download, enabled = !state.capturing && !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { state.capture() }
        }
    }
}

private fun hostOf(url: String) = runCatching { java.net.URI(url).host }.getOrNull() ?: url

// ---------------------------------------------------------------- review

/** Height of the floating confirm bar on the review screen. */
private val ConfirmBarSpace = 150.dp

@Composable
fun ImportReviewContent(controller: AppController, draft: ImportDraft, padding: PaddingValues) {
    val c = Kiez.colors
    val today = controller.today()
    val selected = remember(draft) { mutableStateListOf<Boolean>().apply { repeat(draft.items.size) { add(true) } } }
    var withMarket by remember(draft) { mutableStateOf(draft.detectedPostalCodes.size == 1) }
    var postal by remember(draft) { mutableStateOf(draft.detectedPostalCodes.singleOrNull().orEmpty()) }
    var marketName by remember(draft) { mutableStateOf("") }
    var error by remember(draft) { mutableStateOf<String?>(null) }
    val chosen = draft.items.filterIndexed { i, _ -> selected.getOrElse(i) { false } }
    val postalValid = PostalCodes.isValid(postal)
    val portal = draft.site?.isPortal == true
    val canSave = draft.site != null && chosen.isNotEmpty() && (!withMarket || postalValid)
    val chainArea = draft.retailer?.let(WebImport::chainArea)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + if (draft.items.isNotEmpty()) ConfirmBarSpace else 0.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Erkannte Preise", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${Labels.count(draft.items.size, "Preis", "Preise")} auf ${draft.host}. Vergleiche ein paar davon mit der Seite. " +
                            "Stimmt etwas nicht, nimm den Haken raus oder übernimm gar nichts." +
                            if (draft.unassigned > 0) " ${Labels.count(draft.unassigned, "weiterer Preis ist", "weitere Preise sind")} keinem Händler eindeutig zuzuordnen und fehlen hier." else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            when {
                draft.site == null -> item {
                    StateMessage(Icons.Rounded.SearchOff, "Kein unterstützter Händler",
                        "${draft.host} gehört zu keinem der Händler, die Kiezpreis kennt. Von dieser Seite wird nichts übernommen.")
                }
                draft.items.isEmpty() -> item {
                    StateMessage(
                        Icons.Rounded.SearchOff, "Keine Preise erkannt",
                        "Auf dieser Seite hat Kiezpreis keine eindeutig lesbaren Preise gefunden. Öffne die Angebotsliste deines Markts " +
                            "(nicht die Startseite), scrolle einmal bis unten und versuche es noch einmal. Unklare Angaben werden absichtlich nicht übernommen.",
                    ) { SecondaryButton("Zurück zur Seite") { controller.back() } }
                }
                else -> {
                    item {
                        MarketCard(
                            draft, withMarket, postal, marketName, postalValid, chainArea, portal,
                            onWithMarket = { withMarket = it; error = null },
                            onPostal = { postal = it.filter(Char::isDigit).take(5); error = null },
                            onName = { marketName = it.take(60) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item {
                        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${chosen.size} von ${draft.items.size} ausgewählt", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text("Alle", color = c.accent, style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clickable(role = Role.Button) { selected.indices.forEach { selected[it] = true } }.padding(8.dp))
                            Text("Keine", color = c.accent, style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clickable(role = Role.Button) { selected.indices.forEach { selected[it] = false } }.padding(8.dp))
                        }
                    }
                    itemsIndexed(draft.items) { i, item ->
                        ExtractedRow(item, selected.getOrElse(i) { false }, portal, Modifier.padding(horizontal = 16.dp)) { selected[i] = !selected[i] }
                    }
                }
            }
        }

        if (draft.site != null && draft.items.isNotEmpty()) {
            GlassSurface(
                Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding()).fillMaxWidth(),
                blur = true,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        error ?: when {
                            withMarket && !postalValid -> "Gib die Postleitzahl des Markts an."
                            withMarket && portal -> "Wird als Preis für die Umgebung von $postal gespeichert, wie ${draft.site?.name} ihn zeigt."
                            withMarket -> "Wird als Preis dieses einen Markts gespeichert (${PostalCodes.areaLabel(postal)})."
                            chainArea != null -> "Wird als Preis im $chainArea gespeichert."
                            else -> "Wird mit „Gebiet unbekannt“ gespeichert."
                        },
                        style = MaterialTheme.typography.bodySmall, color = if (error != null) c.danger else c.textSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("${Labels.count(chosen.size, "Preis", "Preise")} übernehmen", enabled = canSave, modifier = Modifier.fillMaxWidth()) {
                        val market = if (withMarket) MarketChoice.Market(postal, marketName.ifBlank { null }) else MarketChoice.Unknown
                        error = controller.confirmImport(chosen, market)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketCard(
    draft: ImportDraft,
    withMarket: Boolean,
    postal: String,
    name: String,
    postalValid: Boolean,
    chainArea: String?,
    portal: Boolean,
    onWithMarket: (Boolean) -> Unit,
    onPostal: (String) -> Unit,
    onName: (String) -> Unit,
    modifier: Modifier,
) {
    val c = Kiez.colors
    FrostCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Store, null, tint = c.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (portal) "Für welche Gegend hast du gesucht?" else "Für welchen Markt gelten die Preise?", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KChip(if (portal) "PLZ angeben" else "Ein bestimmter Markt", selected = withMarket) { onWithMarket(true) }
                KChip(if (chainArea != null) "Ganzes Gebiet" else "Weiß ich nicht", selected = !withMarket) { onWithMarket(false) }
            }
            Spacer(Modifier.height(10.dp))
            if (withMarket) {
                InputField(postal, "PLZ des Markts, z. B. 13591", "Postleitzahl des Markts", KeyboardType.Number, onPostal)
                if (postal.length == 5 && !postalValid) {
                    Text("$postal ist keine gültige Postleitzahl.",
                        style = MaterialTheme.typography.labelMedium, color = c.danger, modifier = Modifier.padding(top = 6.dp))
                }
                if (draft.detectedPostalCodes.size == 1 && postal == draft.detectedPostalCodes.single()) {
                    Text("Von der Seite übernommen. Bitte prüfen.", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(8.dp))
                if (portal) {
                    Text("Die PLZ, die du auf dem Portal eingegeben hast. Die Preise gelten dann als Preise dieser Gegend, nicht eines bestimmten Markts.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    InputField(name, "Name des Markts (optional)", "Name des Markts", KeyboardType.Text, onName)
                    Spacer(Modifier.height(8.dp))
                    Text("Die Preise gelten dann nur für diesen Markt. Andere Filialen können abweichen.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    if (chainArea != null) "Die Preise gelten dann im $chainArea, wie es die Seite von ${draft.retailer?.shortName} zeigt."
                    else "Die Preise werden mit „Gebiet unbekannt“ gekennzeichnet. Du siehst bei jedem Angebot, dass der Ort nicht belegt ist.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun InputField(value: String, placeholder: String, description: String, keyboard: KeyboardType, onChange: (String) -> Unit) {
    val c = Kiez.colors
    Box(Modifier.fillMaxWidth().background(c.divider, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (value.isEmpty()) Text(placeholder, color = c.textTertiary, fontSize = 15.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        )
    }
}

@Composable
private fun ExtractedRow(item: ExtractedOffer, checked: Boolean, showRetailer: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val c = Kiez.colors
    FrostCard(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(role = Role.Checkbox, onClick = onToggle)
                .semantics { stateDescription = if (checked) "ausgewählt" else "nicht ausgewählt" }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null,
                tint = if (checked) c.accent else c.textTertiary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            if (showRetailer && item.retailer != null) {
                RetailerMark(item.retailer!!, size = 30.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                item.brand?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary) }
                Text(item.title, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val details = listOfNotNull(
                    item.sizeText,
                    item.basePriceText,
                    ValidityRules.periodLabel(Validity(item.validFrom, item.validTo)),
                ).joinToString(" · ")
                if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Pill(item.evidence.label, c.textSecondary, c.divider)
                    item.conditions.firstOrNull()?.let { Pill(it.label, c.caution, c.cautionSoft) }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(item.price.format(), style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                item.referencePrice?.let { StrikePrice(it.price) }
            }
        }
    }
}
