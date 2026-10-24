package de.kiezpreis.core.importer

import de.kiezpreis.core.data.FetchContext
import de.kiezpreis.core.data.FetchResult
import de.kiezpreis.core.data.JsonFileStore
import de.kiezpreis.core.data.OfferSource
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.data.SourceDescriptor
import de.kiezpreis.core.logic.PackSizeParser
import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Store
import de.kiezpreis.core.model.Validity
import de.kiezpreis.core.region.Berlin
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
    val retailer: Retailer?,
    val items: List<ExtractedOffer>,
    /** Berlin postal codes the page itself mentions as market location. Only used to prefill. */
    val detectedPostalCodes: List<String>,
    val capturedAt: Long,
)

/** Which market the user says the prices belong to. */
sealed interface MarketChoice {
    /** The user does not know or the page shows chain-wide prices: Berlin stays unconfirmed. */
    data object Unknown : MarketChoice
    data class BerlinMarket(val postalCode: String, val name: String?) : MarketChoice
}

/** Prices the user took over from one retailer page. */
@Serializable
data class WebImportBatch(
    val retailer: Retailer,
    val pageUrl: String,
    val store: Store? = null,
    val importedAt: Long,
    val offers: List<Offer>,
)

object WebImport {

    const val SOURCE_ID = "web_import"

    /** Imports without an end date are no longer shown after this many days. */
    const val UNDATED_MAX_AGE_DAYS = 7L

    private val hosts = mapOf(
        "aldi-nord.de" to Retailer.ALDI_NORD,
        "lidl.de" to Retailer.LIDL,
        "rewe.de" to Retailer.REWE,
        "edeka.de" to Retailer.EDEKA,
        "kaufland.de" to Retailer.KAUFLAND,
        "penny.de" to Retailer.PENNY,
        "netto-online.de" to Retailer.NETTO_MD,
    )

    fun retailerForHost(host: String): Retailer? {
        val h = host.lowercase().trimEnd('.')
        return hosts.entries.firstOrNull { (domain, _) -> h == domain || h.endsWith(".$domain") }?.value
    }

    /** Whether the in-app browser may navigate to [host] while showing [retailer]'s site. */
    fun isAllowedHost(retailer: Retailer, host: String): Boolean = retailerForHost(host) == retailer

    fun draft(page: CapturedPage, now: Long): ImportDraft = ImportDraft(
        pageUrl = page.url,
        host = page.host,
        retailer = retailerForHost(page.host),
        items = OfferExtractor.extract(page),
        detectedPostalCodes = detectPostalCodes(page),
        capturedAt = now,
    )

    /**
     * Turns confirmed items into offers. Fails when the page is not a known Berlin retailer
     * or the chosen market is not in Berlin; nothing is stored in that case.
     */
    fun toBatch(draft: ImportDraft, selected: List<ExtractedOffer>, market: MarketChoice, now: Long): Result<WebImportBatch> {
        val retailer = draft.retailer ?: return Result.failure(IllegalArgumentException("Seite gehört zu keinem unterstützten Händler"))
        val store = when (market) {
            MarketChoice.Unknown -> null
            is MarketChoice.BerlinMarket -> {
                if (!Berlin.containsPostalCode(market.postalCode)) {
                    return Result.failure(IllegalArgumentException("${market.postalCode} ist keine Berliner Postleitzahl"))
                }
                Store(
                    id = "${retailer.name.lowercase()}_${market.postalCode}_${hash(market.name.orEmpty()).take(6)}",
                    retailer = retailer,
                    name = market.name?.trim()?.takeIf { it.isNotEmpty() } ?: "${retailer.shortName} ${market.postalCode}",
                    postalCode = market.postalCode,
                    city = "Berlin",
                )
            }
        }
        val availability = if (store != null) BerlinAvailability.STORE_SPECIFIC else BerlinAvailability.UNCONFIRMED
        val offers = selected.map { e ->
            Offer(
                id = "web_${retailer.name.lowercase()}_${hash(listOf(e.title, e.price.value, store?.id, e.validFrom).joinToString("|")).take(16)}",
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
                availability = availability,
                stores = listOfNotNull(store),
                conditions = e.conditions.toList(),
                imageUrl = e.imageUrl,
                sourceUrl = draft.pageUrl,
                ean = e.ean,
                fetchedAt = now,
            )
        }
        return Result.success(WebImportBatch(retailer, draft.pageUrl, store, now, offers))
    }

    /** Distinct Berlin postal codes found under address-like keys in the page data. */
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
                        if (Berlin.containsPostalCode(code)) found += code
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
        val rest = _batches.value.filterNot { it.retailer == batch.retailer && it.store?.id == batch.store?.id }
        write(rest + batch)
    }

    @Synchronized
    fun remove(retailer: Retailer) = write(_batches.value.filterNot { it.retailer == retailer })

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
