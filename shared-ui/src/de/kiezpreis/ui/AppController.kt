package de.kiezpreis.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.kiezpreis.core.data.Favorite
import de.kiezpreis.core.data.FetchContext
import de.kiezpreis.core.data.FetchResult
import de.kiezpreis.core.data.OfferRepository
import de.kiezpreis.core.data.UserDataRepository
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.search.Grouping
import de.kiezpreis.core.search.ProductGroup
import de.kiezpreis.core.search.ProductIdentity
import de.kiezpreis.core.search.SearchEngine
import de.kiezpreis.core.search.SearchFilters
import de.kiezpreis.core.search.SortOrder
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.importer.CapturedPage
import de.kiezpreis.core.importer.ExtractedOffer
import de.kiezpreis.core.importer.ImportDraft
import de.kiezpreis.core.importer.MarketChoice
import de.kiezpreis.core.importer.WebImport
import de.kiezpreis.core.importer.WebImportStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

enum class Tab(val label: String) { HOME("Start"), SEARCH("Suche"), FAVORITES("Merkliste"), SETTINGS("Einstellungen") }

/** Opened product: identified by its product key; resolved against current data on every render. */
data class DetailRoute(val productKey: String, val fallbackTitle: String)

/**
 * UI state holder shared by Android and the desktop preview. Plain Kotlin + Compose snapshot state;
 * on Android it is kept alive across configuration changes by a ViewModel wrapper.
 */
class AppController(
    val offers: OfferRepository,
    val user: UserDataRepository,
    val imports: WebImportStore,
    private val scope: CoroutineScope,
    val now: () -> Long = System::currentTimeMillis,
) {
    val engine = SearchEngine { today() }

    fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(BerlinZone).toLocalDate()

    var tab by mutableStateOf(Tab.HOME)
        private set
    val detailStack = mutableStateListOf<DetailRoute>()
    var filterSheetOpen by mutableStateOf(false)
    var sourcesScreenOpen by mutableStateOf(false)

    var query by mutableStateOf("")
    var filters by mutableStateOf(SearchFilters())
    var sort by mutableStateOf(SortOrder.RELEVANCE)
    /** Increments whenever the search field should take focus. */
    var searchFocusRequest by mutableStateOf(0)

    var testResults by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** Open retailer page, above the sources screen. */
    var browser by mutableStateOf<BrowserState?>(null)
        private set
    /** Prices read from the page, waiting for the user's confirmation. */
    var importDraft by mutableStateOf<ImportDraft?>(null)
        private set
    /** One-line feedback after an import, shown on the sources screen. */
    var importMessage by mutableStateOf<String?>(null)

    init {
        scope.launch {
            // Imported prices are always used: the user brought them in deliberately.
            user.prefs.collect { offers.setEnabledSources(it.enabledSources + SourceCatalog.webImport.id) }
        }
        refresh(force = false)
    }

    fun selectTab(t: Tab) {
        detailStack.clear()
        closeBrowser()
        sourcesScreenOpen = false
        tab = t
    }

    fun startSearch(focus: Boolean = true) {
        selectTab(Tab.SEARCH)
        if (focus) searchFocusRequest++
    }

    fun submitSearch(q: String) {
        query = q
        user.addRecentSearch(q)
        if (tab != Tab.SEARCH) selectTab(Tab.SEARCH)
    }

    fun browseCategory(category: Category) {
        query = ""
        filters = SearchFilters(categories = setOf(category))
        selectTab(Tab.SEARCH)
    }

    fun browseRetailer(retailer: Retailer) {
        query = ""
        filters = SearchFilters(retailers = setOf(retailer))
        selectTab(Tab.SEARCH)
    }

    fun openProduct(group: ProductGroup) {
        detailStack.add(DetailRoute(group.key, group.title))
    }

    fun openFavorite(f: Favorite) {
        detailStack.add(DetailRoute(f.productKey, f.title))
    }

    /** Returns true if something was closed. */
    fun back(): Boolean = when {
        filterSheetOpen -> { filterSheetOpen = false; true }
        detailStack.isNotEmpty() -> { detailStack.removeAt(detailStack.lastIndex); true }
        importDraft != null -> { importDraft = null; browser?.capturing = false; true }
        browser != null -> {
            val b = browser!!
            if (b.canGoBack) b.goBack() else closeBrowser()
            true
        }
        sourcesScreenOpen -> { sourcesScreenOpen = false; true }
        tab != Tab.HOME -> { tab = Tab.HOME; true }
        else -> false
    }

    fun refresh(force: Boolean) {
        scope.launch { offers.refresh(force, FetchContext(now(), user.prefs.value.postalCode)) }
    }

    fun testSource(id: String) {
        testResults = testResults + (id to "Teste …")
        scope.launch {
            val text = when (val r = offers.test(id)) {
                is FetchResult.Success -> "Erreichbar: ${Labels.count(r.offers.size, "Preis", "Preise")} gelesen" +
                    if (r.rejected > 0) ", ${r.rejected} verworfen (ohne Ort, Fremdwährung oder unvollständig)" else ""
                is FetchResult.Failure -> "${r.reason.label}: ${r.message}"
            }
            testResults = testResults + (id to text)
        }
    }

    fun toggleSource(id: String, enabled: Boolean) {
        user.updatePrefs { p -> p.copy(enabledSources = if (enabled) p.enabledSources + id else p.enabledSources - id) }
        if (enabled) refresh(force = false)
    }

    fun openRetailerBrowser(retailer: Retailer) {
        importMessage = null
        browser = BrowserState(retailer, retailer.website, ::onCaptured)
    }

    fun closeBrowser() {
        importDraft = null
        browser = null
    }

    private fun onCaptured(raw: String) {
        scope.launch(Dispatchers.Default) {
            val draft = runCatching { WebImport.draft(CapturedPage.decode(raw), now()) }.getOrNull()
            val b = browser
            if (draft == null) {
                b?.capturing = false
                b?.error = "Die Seite konnte nicht gelesen werden."
            } else {
                importDraft = draft
            }
        }
    }

    /** Stores the confirmed prices. Returns an error text, or null on success. */
    fun confirmImport(selected: List<ExtractedOffer>, market: MarketChoice): String? {
        val draft = importDraft ?: return "Nichts zu übernehmen"
        val batch = WebImport.toBatch(draft, selected, market, now()).getOrElse { return it.message ?: "Übernahme nicht möglich" }
        imports.save(batch)
        importMessage = "${Labels.count(batch.offers.size, "Preis", "Preise")} von ${batch.retailer.displayName} übernommen" +
            (batch.store?.let { " · ${it.name}" } ?: " · ${WebImport.chainArea(batch.retailer) ?: "Gebiet unbekannt"}")
        closeBrowser()
        sourcesScreenOpen = true
        scope.launch { offers.refresh(force = true, context = FetchContext(now(), user.prefs.value.postalCode), only = setOf(WebImport.SOURCE_ID)) }
        return null
    }

    fun removeImport(retailer: Retailer) {
        imports.remove(retailer)
        importMessage = null
        scope.launch { offers.refresh(force = true, context = FetchContext(now(), null), only = setOf(WebImport.SOURCE_ID)) }
    }

    /** "Nur meine Gegend": needs a postal code; without one the settings open instead. */
    fun toggleNearby() {
        val pc = user.prefs.value.postalCode
        when {
            filters.nearPostalCode != null -> filters = filters.copy(nearPostalCode = null)
            pc != null -> filters = filters.copy(nearPostalCode = pc)
            else -> { filterSheetOpen = false; selectTab(Tab.SETTINGS) }
        }
    }

    /** Product group for a key, built from all current offers (not limited by search filters). */
    fun resolveGroup(productKey: String, all: List<Offer>): ProductGroup? {
        val current = all.filter { de.kiezpreis.core.logic.ValidityRules.isCurrent(it, today()) }
        return Grouping.group(current).firstOrNull { it.key == productKey }
    }

    fun toggleFavorite(group: ProductGroup) {
        user.toggleFavorite(
            Favorite(
                productKey = group.key,
                title = group.title,
                brand = group.brand,
                searchQuery = listOfNotNull(group.brand, group.title).joinToString(" "),
                addedAt = now(),
            ),
        )
    }

    fun keyOf(offer: Offer) = ProductIdentity.key(offer).id
}
