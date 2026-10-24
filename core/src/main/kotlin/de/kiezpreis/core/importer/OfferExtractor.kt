package de.kiezpreis.core.importer

import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.OfferCondition
import de.kiezpreis.core.model.ReferencePrice
import de.kiezpreis.core.model.ReferencePriceKind
import de.kiezpreis.core.search.TextNormalizer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How an extracted price was found. Shown in the review list so the user can judge it. */
enum class Evidence(val label: String, val rank: Int) {
    STRUCTURED("Produktdaten der Seite", 3),
    PAGE_DATA("Daten der Seite", 2),
    PAGE_TEXT("Text der Seite", 1),
}

data class ExtractedOffer(
    val title: String,
    val brand: String? = null,
    val price: Cents,
    val referencePrice: ReferencePrice? = null,
    val basePriceText: String? = null,
    val sizeText: String? = null,
    val validFrom: String? = null,
    val validTo: String? = null,
    val ean: String? = null,
    val imageUrl: String? = null,
    val conditions: Set<OfferCondition> = emptySet(),
    val evidence: Evidence,
)

/**
 * Reads prices from what a retailer page contains. Deliberately strict: a record is only taken
 * when a product name and exactly one current price can be read without guessing. Anything
 * ambiguous (bare integers that could be euro or cents, several prices in one tile, foreign
 * currency) is dropped instead of interpreted.
 */
object OfferExtractor {

    private val json = Json { isLenient = true }
    private const val MAX_NODES = 300_000
    private const val MAX_RESULTS = 1_500
    private val MIN_PRICE = Cents(5)
    private val MAX_PRICE = Cents(99_900)

    fun extract(page: CapturedPage): List<ExtractedOffer> {
        val found = mutableListOf<ExtractedOffer>()
        page.jsonLd.forEach { parse(it)?.let { root -> walk(root, Evidence.STRUCTURED, found) } }
        page.embeddedJson.forEach { parse(it)?.let { root -> walk(root, Evidence.PAGE_DATA, found) } }
        page.responses.forEach { r -> parse(r.body)?.let { root -> walk(root, Evidence.PAGE_DATA, found) } }
        page.domCards.forEach { card -> fromCard(card)?.let(found::add) }
        return dedupe(found).take(MAX_RESULTS)
    }

    private fun parse(text: String): JsonElement? = runCatching { json.parseToJsonElement(text.trim()) }.getOrNull()

    // ---------------------------------------------------------------- JSON

    private fun walk(root: JsonElement, evidence: Evidence, out: MutableList<ExtractedOffer>) {
        var visited = 0
        val stack = ArrayDeque<Pair<JsonElement, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            visited++
            when (node) {
                is JsonObject -> {
                    fromObject(node, evidence)?.let(out::add)
                    // JSON-LD often wraps items in "@graph"; any nested JSON string is not followed on purpose.
                    if (depth < 40) node.values.reversed().forEach { stack.addLast(it to depth + 1) }
                }
                // Pushed in reverse so items come out in page order.
                is JsonArray -> if (depth < 40) node.reversed().forEach { stack.addLast(it to depth + 1) }
                else -> Unit
            }
        }
    }

    private fun fromObject(o: JsonObject, evidence: Evidence): ExtractedOffer? {
        val title = o.field(TITLE_KEYS)?.string()?.let(::cleanText)?.takeIf { it.length in 2..160 } ?: return null
        val currency = o.field(CURRENCY_KEYS)?.string() ?: priceContainers(o).firstNotNullOfOrNull { it.field(CURRENCY_KEYS)?.string() }
        if (currency != null && !currency.equals("EUR", true) && currency != "€") return null

        val price = currentPrice(o) ?: return null
        if (price < MIN_PRICE || price > MAX_PRICE) return null

        val ref = referencePrice(o)?.takeIf { it.price > price }
        val (from, to) = validity(o)
        return ExtractedOffer(
            title = title,
            brand = o.field(BRAND_KEYS)?.let { it.string() ?: (it as? JsonObject)?.field(listOf("name"))?.string() }
                ?.let(::cleanText)?.takeIf { it.length in 1..60 },
            price = price,
            referencePrice = ref,
            basePriceText = (o.field(BASE_KEYS) ?: priceContainers(o).firstNotNullOfOrNull { it.field(BASE_KEYS) })
                ?.let { it.string() ?: (it as? JsonObject)?.field(TEXT_KEYS)?.string() }
                ?.let(::cleanText)?.takeIf { it.any(Char::isDigit) && it.length <= 80 },
            sizeText = o.field(SIZE_KEYS)?.string()?.let(::cleanText)?.takeIf { it.any(Char::isDigit) && it.length <= 80 },
            validFrom = from,
            validTo = to,
            ean = o.field(EAN_KEYS)?.let { (it as? JsonArray)?.firstOrNull() ?: it }?.string()
                ?.filter(Char::isDigit)?.takeIf { it.length == 8 || it.length == 13 },
            imageUrl = image(o.field(IMAGE_KEYS)),
            conditions = conditions(o),
            evidence = evidence,
        )
    }

    /** The current price: directly on the object, or inside a price/offer container one level down. */
    private fun currentPrice(o: JsonObject): Cents? {
        o.entryFor(CURRENT_PRICE_KEYS)?.let { (k, v) -> money(k, v)?.let { return it } }
        for (container in priceContainers(o)) {
            container.entryFor(CURRENT_PRICE_KEYS + VALUE_KEYS)?.let { (k, v) -> money(k, v)?.let { return it } }
        }
        return null
    }

    private fun referencePrice(o: JsonObject): ReferencePrice? {
        val scopes = listOf(o) + priceContainers(o)
        for ((keys, kind) in REFERENCE_KEYS) {
            for (scope in scopes) {
                scope.entryFor(keys)?.let { (k, v) -> money(k, v)?.let { return ReferencePrice(it, kind) } }
            }
        }
        return null
    }

    private fun priceContainers(o: JsonObject): List<JsonObject> =
        CONTAINER_KEYS.mapNotNull { key ->
            when (val v = o.field(listOf(key))) {
                is JsonObject -> v
                is JsonArray -> v.firstOrNull() as? JsonObject
                else -> null
            }
        } + listOfNotNull(o.field(CURRENT_PRICE_KEYS) as? JsonObject)

    /**
     * Converts a JSON value into money. Decimal numbers and price strings are euro; whole numbers
     * only count when the key says they are cents. A bare `199` is ambiguous and therefore ignored.
     */
    internal fun money(key: String, v: JsonElement): Cents? = when (v) {
        is JsonPrimitive -> when {
            v is JsonNull -> null
            v.isString -> if (norm(key).contains("cent") && v.content.trim().all(Char::isDigit) && v.content.isNotBlank()) Cents(v.content.trim().toLong())
            else PriceText.parse(v.content)
            norm(key).contains("cent") -> v.content.toLongOrNull()?.let(::Cents)
            v.content.contains('.') -> v.content.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(Cents::ofEuro)
            else -> null
        }
        is JsonObject -> v.entryFor(VALUE_KEYS)?.let { (k, inner) -> if (inner is JsonObject) null else money(k, inner) }
        else -> null
    }

    private fun validity(o: JsonObject): Pair<String?, String?> {
        val scopes = listOf(o) + VALIDITY_CONTAINERS.mapNotNull { o.field(listOf(it)) as? JsonObject } + priceContainers(o)
        val from = scopes.firstNotNullOfOrNull { s -> s.field(FROM_KEYS)?.let(::date) }
        val to = scopes.firstNotNullOfOrNull { s -> s.field(TO_KEYS)?.let(::date) }
        return if (from != null && to != null && to < from) null to null else from to to
    }

    internal fun date(v: JsonElement): String? {
        val p = v as? JsonPrimitive ?: return null
        if (p is JsonNull) return null
        val text = p.content.trim()
        ISO_DATE.find(text)?.let { m -> return runCatching { LocalDate.parse(m.value).toString() }.getOrNull() }
        GERMAN_DATE.matchEntire(text)?.let { m ->
            val (d, mo, y) = m.destructured
            return runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()).toString() }.getOrNull()
        }
        val n = text.toLongOrNull() ?: return null
        val millis = when (n) {
            in 1_500_000_000_000..2_500_000_000_000 -> n
            in 1_500_000_000..2_500_000_000 -> n * 1000
            else -> return null
        }
        return Instant.ofEpochMilli(millis).atZone(BERLIN).toLocalDate().toString()
    }

    private fun image(v: JsonElement?): String? = when (v) {
        is JsonPrimitive -> v.contentOrNullIfNull()?.takeIf { it.startsWith("https://") }
        is JsonArray -> image(v.firstOrNull())
        is JsonObject -> image(v.field(listOf("url", "src", "href", "contentUrl")))
        else -> null
    }

    private fun conditions(o: JsonObject): Set<OfferCondition> {
        val texts = mutableListOf<String>()
        fun collect(e: JsonElement, depth: Int) {
            if (texts.size > 60) return
            when (e) {
                is JsonPrimitive -> if (e.isString && e.content.length < 300) texts += e.content.lowercase()
                is JsonObject -> if (depth < 2) e.values.forEach { collect(it, depth + 1) }
                is JsonArray -> if (depth < 2) e.forEach { collect(it, depth + 1) }
            }
        }
        collect(o, 0)
        return conditionsFromText(texts.joinToString(" | "))
    }

    internal fun conditionsFromText(lower: String): Set<OfferCondition> = buildSet {
        if (APP_ONLY.containsMatchIn(lower)) add(OfferCondition.APP_OR_CARD_ONLY)
        if ("coupon" in lower) add(OfferCondition.COUPON)
        if ("teilnehmenden" in lower) add(OfferCondition.PARTICIPATING_STORES)
        if ("pfand" in lower) add(OfferCondition.PLUS_DEPOSIT)
        if ("solange der vorrat" in lower || "solange vorrat" in lower) add(OfferCondition.WHILE_STOCKS_LAST)
        if (MULTI_BUY.containsMatchIn(lower)) add(OfferCondition.MULTI_BUY)
    }

    // ---------------------------------------------------------------- DOM

    private fun fromCard(card: DomCard): ExtractedOffer? {
        val title = cleanText(card.title).takeIf { it.length in 2..160 } ?: return null
        val price = PriceText.parse(card.price) ?: return null
        if (price < MIN_PRICE || price > MAX_PRICE) return null
        val old = card.oldPrice?.let(PriceText::parse)?.takeIf { it > price }
        val oldKind = card.oldPrice?.lowercase()?.let {
            when {
                "uvp" in it -> ReferencePriceKind.RRP
                "30" in it && "tag" in it -> ReferencePriceKind.LOWEST_30_DAYS
                else -> ReferencePriceKind.STRIKE_THROUGH
            }
        }
        return ExtractedOffer(
            title = title,
            brand = card.brand?.let(::cleanText)?.takeIf { it.length in 1..60 },
            price = price,
            referencePrice = if (old != null && oldKind != null) ReferencePrice(old, oldKind) else null,
            basePriceText = card.basePrice?.let(::cleanText)?.takeIf { it.any(Char::isDigit) && it.length <= 80 },
            sizeText = card.size?.let(::cleanText)?.takeIf { it.any(Char::isDigit) && it.length <= 80 },
            imageUrl = card.image?.takeIf { it.startsWith("https://") },
            conditions = conditionsFromText(listOfNotNull(card.note, card.price, card.title).joinToString(" | ").lowercase()),
            evidence = Evidence.PAGE_TEXT,
        )
    }

    // ---------------------------------------------------------------- helpers

    /** One entry per product name and price. The better evidenced record wins. */
    private fun dedupe(list: List<ExtractedOffer>): List<ExtractedOffer> {
        val best = LinkedHashMap<String, ExtractedOffer>()
        for (o in list) {
            val key = TextNormalizer.normalizeQuery(o.title) + "|" + o.price.value
            val existing = best[key]
            if (existing == null || o.evidence.rank > existing.evidence.rank) best[key] = o
        }
        return best.values.toList()
    }

    internal fun cleanText(s: String): String =
        s.replace(Regex("<[^>]{0,200}>"), " ")
            .replace("&amp;", "&").replace("&nbsp;", " ").replace("&shy;", "").replace("­", "")
            .replace(Regex("\\s+"), " ").trim()

    private fun norm(key: String) = key.lowercase().replace("_", "").replace("-", "")

    private fun JsonObject.entryFor(keys: List<String>): Pair<String, JsonElement>? {
        for (k in keys) for ((key, value) in this) {
            // "oldPriceInCents" matches "oldprice"; money() still sees the original key and reads cents.
            if ((norm(key) == k || norm(key).replace(CENT_SUFFIX, "") == k) && value !is JsonNull) return key to value
        }
        return null
    }

    private fun JsonObject.field(keys: List<String>): JsonElement? = entryFor(keys)?.second

    private fun JsonElement.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

    private fun JsonPrimitive.contentOrNullIfNull(): String? = if (this is JsonNull) null else content

    private val CENT_SUFFIX = Regex("(incents?|cents?)$")
    private val BERLIN: ZoneId = ZoneId.of("Europe/Berlin")
    private val ISO_DATE = Regex("""^\d{4}-\d{2}-\d{2}""")
    private val GERMAN_DATE = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""")
    private val APP_ONLY = Regex("""\b(app|kundenkarte|payback|lidl plus|kaufland card|deutschlandcard)\b""")
    private val MULTI_BUY = Regex("""\b(ab \d+ (stück|stk|packungen|flaschen|dosen)|beim kauf von \d+)""")

    private val TITLE_KEYS = listOf("name", "title", "productname", "producttitle", "headline", "displayname", "articlename", "productdisplayname")
    private val CURRENCY_KEYS = listOf("currency", "pricecurrency", "currencycode")
    private val CURRENT_PRICE_KEYS = listOf(
        "price", "currentprice", "offerprice", "saleprice", "salesprice", "pricevalue", "finalprice", "discountedprice",
        "promotionprice", "actionprice", "specialprice", "priceincents", "priceincent", "currentpriceincents", "pricecents",
    )
    private val VALUE_KEYS = listOf("value", "amount", "current", "price", "formatted", "display", "text")
    private val CONTAINER_KEYS = listOf("prices", "pricedata", "priceinfo", "pricing", "pricedetails", "priceinformation", "offers", "offer")
    private val REFERENCE_KEYS = listOf(
        listOf("lowestprice30days", "lowestpricelast30days", "lowest30dayprice", "lowestprice", "thirtydaylowestprice") to ReferencePriceKind.LOWEST_30_DAYS,
        listOf(
            "oldprice", "originalprice", "strikeprice", "strikethroughprice", "crossedoutprice", "regularprice", "previousprice",
            "wasprice", "priceold", "listprice", "priceregular", "formerprice", "normalprice", "regular", "old", "before",
        ) to ReferencePriceKind.STRIKE_THROUGH,
        listOf("uvp", "rrp", "recommendedretailprice") to ReferencePriceKind.RRP,
    )
    private val BASE_KEYS = listOf(
        "baseprice", "basicprice", "grammage", "unitprice", "priceperunit", "grundpreis", "unitpricetext",
        "basepricetext", "comparisonprice", "basepriceformatted",
    )
    private val TEXT_KEYS = listOf("text", "formatted", "display", "label")
    private val SIZE_KEYS = listOf("packaging", "packagesize", "packsize", "size", "quantitytext", "contenttext", "weight", "subtitle")
    private val BRAND_KEYS = listOf("brand", "brandname", "manufacturer")
    private val FROM_KEYS = listOf("validfrom", "startdate", "validitystart", "datefrom", "offervalidfrom", "pricevalidfrom", "from", "start")
    private val TO_KEYS = listOf("validto", "validuntil", "enddate", "validityend", "dateto", "offervalidto", "pricevaliduntil", "to", "end")
    private val VALIDITY_CONTAINERS = listOf("validity", "period", "validityperiod", "promotionperiod", "offerperiod", "dates")
    private val EAN_KEYS = listOf("ean", "gtin", "gtin13", "gtin8", "eancode", "eans")
    private val IMAGE_KEYS = listOf("image", "imageurl", "img", "picture", "thumbnail", "images")
}

/** Reads a single price from text such as "1,99 €", "1.99", "-,99" or "2,-". */
object PriceText {
    private val PRICE = Regex("""(?<![\d.,])(\d{1,3}|[-–])\s?[.,]\s?(\d{2}|[-–]{1,2})(?!\d|[.,]\d)""")

    /** Null when the text has no price or several different ones. */
    fun parse(text: String): Cents? {
        val values = PRICE.findAll(text).mapNotNull { m ->
            val (euros, cents) = m.destructured
            val e = if (euros == "-" || euros == "–") 0L else euros.toLong()
            val c = if (cents.startsWith("-") || cents.startsWith("–")) 0L else cents.toLong()
            if ((euros == "-" || euros == "–") && c == 0L) null else Cents(e * 100 + c)
        }.toSet()
        return values.singleOrNull()
    }
}
