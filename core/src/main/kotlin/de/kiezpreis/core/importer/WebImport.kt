package de.kiezpreis.core.importer

import de.kiezpreis.core.data.FetchContext
import de.kiezpreis.core.data.FetchResult
import de.kiezpreis.core.data.JsonFileStore
import de.kiezpreis.core.data.OfferSource
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.data.SourceDescriptor
import de.kiezpreis.core.logic.PackSizeParser
import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Store
import de.kiezpreis.core.model.Validity
import de.kiezpreis.core.region.Berlin
import de.kiezpreis.core.region.PostalCodes
import de.kiezpreis.core.search.TextNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest

/** Result of reading one page, before the user confirms it. */
data class ImportDraft(
    val pageUrl: String,
    val host: String,
    /** The site the page belongs to; null for a site Kiezpreis does not support. */
    val site: ImportSite?,
    /** Items with a known chain. On a retailer site that is the site's retailer. */
    val items: List<ExtractedOffer>,
    /** Portal items whose chain could not be told unambiguously; they are not offered for import. */
    val unassigned: Int,
    /** Postal codes the page itself mentions as location. Only used to prefill. */
    val detectedPostalCodes: List<String>,
    val capturedAt: Long,
) {
    val retailer: Retailer? get() = site?.retailer
}

/** Where the user says the prices belong. */
sealed interface MarketChoice {
    /** The user does not know, or the page shows chain-wide prices. */
    data object Unknown : MarketChoice
    /** A retailer site: one market. A portal: the area the user searched for on the portal. */
    data class Market(val postalCode: String, val name: String?) : MarketChoice
}

/** Prices the user took over from one page. */
@Serializable
data class WebImportBatch(
    /** Set for retailer sites (also in batches saved before portals existed). */
    val retailer: Retailer? = null,
    val pageUrl: String,
    val store: Store? = null,
    val importedAt: Long,
    val offers: List<Offer>,
    val siteId: String? = null,
    /** For portal imports: the area the user searched for. */
    val areaPostalCode: String? = null,
) {
    /** One batch per site and market; also the key for removing it. */
    val siteKey: String get() = siteId ?: retailer?.name ?: "unknown"
}

object WebImport {

    const val SOURCE_ID = "web_import"

    /** Imports without an end date are no longer shown after this many days. */
    const val UNDATED_MAX_AGE_DAYS = 7L

    fun retailerForHost(host: String): Retailer? = ImportSites.forHost(host)?.retailer

    fun draft(page: CapturedPage, now: Long): ImportDraft {
        val site = ImportSites.forHost(page.host)
        val extracted = OfferExtractor.extract(page)
        val resolved = extracted.map { e -> e.copy(retailer = site?.retailer ?: RetailerNames.find(e.retailerText)) }
        return ImportDraft(
            pageUrl = page.url,
            host = page.host,
            site = site,
            items = resolved.filter { it.retailer != null },
            unassigned = resolved.count { it.retailer == null },
            detectedPostalCodes = detectPostalCodes(page),
            capturedAt = now,
        )
    }

    /**
     * Turns confirmed items into offers. Fails when the page is not a supported site or the
     * postal code is invalid; nothing is stored in that case.
     */
    fun toBatch(draft: ImportDraft, selected: List<ExtractedOffer>, market: MarketChoice, now: Long): Result<WebImportBatch> {
        val site = draft.site ?: return Result.failure(IllegalArgumentException("Seite wird nicht unterstützt"))
        if (market is MarketChoice.Market && !PostalCodes.isValid(market.postalCode)) {
            return Result.failure(IllegalArgumentException("${market.postalCode} ist keine gültige Postleitzahl"))
        }
        val postal = (market as? MarketChoice.Market)?.postalCode
        val store = site.retailer?.let { r ->
            (market as? MarketChoice.Market)?.let { m ->
                Store(
                    id = "${r.name.lowercase()}_${m.postalCode}_${hash(m.name.orEmpty()).take(6)}",
                    retailer = r,
                    name = m.name?.trim()?.takeIf { it.isNotEmpty() } ?: "${r.shortName} ${m.postalCode}",
                    postalCode = m.postalCode,
                    city = if (Berlin.containsPostalCode(m.postalCode)) "Berlin" else null,
                )
            }
        }
        val offers = selected.mapNotNull { e ->
            val retailer = e.retailer ?: site.retailer ?: return@mapNotNull null
            // Retailer site: one market, or the chain's own area (ALDI), or unknown.
            // Portal: the prices the portal showed for the searched area.
            val (coverage, area) = when {
                store != null -> Coverage.STORE to null
                site.isPortal && postal != null -> Coverage.REGIONAL to "Umgebung ${PostalCodes.areaLabel(postal)} (laut ${site.name})"
                chainArea(retailer) != null -> Coverage.REGIONAL to chainArea(retailer)
                else -> Coverage.UNKNOWN to null
            }
            Offer(
                id = "web_${retailer.name.lowercase()}_${hash(listOf(e.title, e.price.value, store?.id, postal, e.validFrom).joinToString("|")).take(16)}",
                sourceId = SOURCE_ID,
                retailer = retailer,
                title = e.title,
                brand = e.brand,
                category = Categorizer.guess(e.title),
                sizeText = e.sizeText,
                packSize = PackSizeParser.parse(e.sizeText) ?: PackSizeParser.parse(e.title),
                price = e.price,
                referencePrice = e.referencePrice,
                statedBasePrice = e.basePriceText,
                validity = Validity(e.validFrom, e.validTo),
                availability = coverage,
                area = area,
                stores = listOfNotNull(store),
                conditions = e.conditions.toList(),
                imageUrl = e.imageUrl,
                sourceUrl = draft.pageUrl,
                ean = e.ean,
                fetchedAt = now,
            )
        }
        return Result.success(WebImportBatch(site.retailer, draft.pageUrl, store, now, offers, site.id, if (site.isPortal) postal else null))
    }

    fun chainArea(retailer: Retailer): String? = when (retailer) {
        Retailer.ALDI_NORD -> "ALDI-Nord-Gebiet"
        Retailer.ALDI_SUED -> "ALDI-Süd-Gebiet"
        else -> null
    }

    /** Distinct postal codes found under address-like keys in the page data. */
    internal fun detectPostalCodes(page: CapturedPage): List<String> {
        val found = LinkedHashSet<String>()
        val parser = kotlinx.serialization.json.Json { isLenient = true }
        fun visit(e: JsonElement, depth: Int) {
            if (depth > 30 || found.size > 10) return
            when (e) {
                is JsonObject -> e.forEach { (k, v) ->
                    val key = k.lowercase().replace("_", "")
                    if (key in POSTAL_KEYS && v is JsonPrimitive) {
                        val code = v.content.trim()
                        if (PostalCodes.isValid(code)) found += code
                    } else visit(v, depth + 1)
                }
                is JsonArray -> e.forEach { visit(it, depth + 1) }
                else -> Unit
            }
        }
        (page.jsonLd + page.embeddedJson + page.responses.map { it.body }).forEach { text ->
            runCatching { parser.parseToJsonElement(text) }.getOrNull()?.let { visit(it, 0) }
        }
        return found.toList()
    }

    private val POSTAL_KEYS = setOf("zipcode", "postalcode", "zip", "plz", "postcode", "postleitzahl")

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}

/** Stores the confirmed imports: the latest one per retailer and market replaces the previous one. */
class WebImportStore(private val store: JsonFileStore) {

    private val _batches = MutableStateFlow(store.read(KEY, ListSerializer(WebImportBatch.serializer())) ?: emptyList())
    val batches: StateFlow<List<WebImportBatch>> = _batches.asStateFlow()

    @Synchronized
    fun save(batch: WebImportBatch) {
        val rest = _batches.value.filterNot {
            it.siteKey == batch.siteKey && it.store?.id == batch.store?.id && it.areaPostalCode == batch.areaPostalCode
        }
        write(rest + batch)
    }

    @Synchronized
    fun remove(siteKey: String) = write(_batches.value.filterNot { it.siteKey == siteKey })

    @Synchronized
    fun clear() = write(emptyList())

    private fun write(list: List<WebImportBatch>) {
        _batches.value = list
        store.write(KEY, ListSerializer(WebImportBatch.serializer()), list)
    }

    private companion object {
        const val KEY = "web_imports"
    }
}

/** Offers the user imported. Local only: this source never touches the network. */
class WebImportSource(private val imports: WebImportStore) : OfferSource {
    override val descriptor: SourceDescriptor = SourceCatalog.webImport

    override suspend fun fetch(context: FetchContext): FetchResult {
        val maxAge = WebImport.UNDATED_MAX_AGE_DAYS * 24 * 3_600_000L
        var dropped = 0
        val offers = imports.batches.value.flatMap { batch ->
            batch.offers.filter { o ->
                val undated = o.validity.from == null && o.validity.to == null
                val keep = !undated || context.nowMillis - batch.importedAt <= maxAge
                if (!keep) dropped++
                keep
            }
        }
        return FetchResult.Success(offers, context.nowMillis, rejected = dropped)
    }
}

/** Rough category from product words; only used for browsing filters, never for comparisons. */
object Categorizer {
    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.SPORTS_NUTRITION to listOf("protein", "whey"),
        Category.MEAT to listOf("hahnchen", "schwein", "rind", "hack", "wurst", "salami", "schinken", "steak", "filet", "bratwurst", "puten"),
        Category.DRINKS to listOf("cola", "wasser", "saft", "bier", "limo", "energy", "wein", "sekt", "tee", "kaffee", "eistee", "schorle", "drink", "pils", "radler"),
        Category.DAIRY to listOf("milch", "joghurt", "kase", "butter", "quark", "sahne", "skyr", "mozzarella", "gouda", "frischkase"),
        Category.FRUIT_VEG to listOf("apfel", "banane", "tomate", "gurke", "kartoffel", "paprika", "salat", "zwiebel", "trauben", "beeren", "orangen", "mohren", "zitrone"),
        Category.BAKERY to listOf("brot", "brotchen", "toast", "baguette", "croissant", "kuchen"),
        Category.SWEETS to listOf("schokolade", "chips", "keks", "gummi", "bonbon", "praline", "riegel"),
        Category.FROZEN to listOf("tiefkuhl", "pizza", "pommes", "frost"),
        Category.HOUSEHOLD to listOf("waschmittel", "spulmittel", "toilettenpapier", "kuchenrolle", "reiniger", "mullbeutel", "weichspuler"),
        Category.CARE to listOf("shampoo", "duschgel", "zahnpasta", "deo", "creme", "windeln", "rasier"),
        Category.PANTRY to listOf("nudeln", "reis", "mehl", "zucker", "olivenol", "sonnenblumenol", "konserve", "mais", "linsen", "mussli", "cornflakes", "senf", "ketchup"),
    )

    fun guess(title: String): Category {
        // German compounds: "Vollmilch" ends with, "Hähnchenbrust" starts with the keyword.
        val tokens = TextNormalizer.tokens(title)
        return rules.firstOrNull { (_, words) -> words.any { w -> tokens.any { it.startsWith(w) || it.endsWith(w) } } }?.first
            ?: Category.OTHER
    }
}
