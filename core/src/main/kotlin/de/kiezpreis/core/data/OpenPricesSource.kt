package de.kiezpreis.core.data

import de.kiezpreis.core.logic.PackSizeParser
import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.MeasureUnit
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PackSize
import de.kiezpreis.core.model.PriceKind
import de.kiezpreis.core.model.ReferencePrice
import de.kiezpreis.core.model.ReferencePriceKind
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Store
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Open Prices (https://prices.openfoodfacts.org): crowd-sourced shelf and receipt prices with the
 * exact store (OpenStreetMap) and date. Documented, free, ODbL licensed.
 *
 * A circle over Germany (it also touches neighbouring countries, filtered by [isGerman]), newest first. Every price belongs to one store and is shown with its location;
 * prices without a usable location (postal code or city) are dropped.
 */
class OpenPricesSource(
    private val http: HttpClient,
    private val maxPages: Int = 5,
    private val maxAgeDays: Long = 21,
) : OfferSource {

    override val descriptor = SourceCatalog.openPrices

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun fetch(context: FetchContext): FetchResult {
        val today = Instant.ofEpochMilli(context.nowMillis).atZone(BERLIN_ZONE).toLocalDate()
        val since = today.minusDays(maxAgeDays)
        val offers = mutableListOf<Offer>()
        var rejected = 0
        for (page in 1..maxPages) {
            val url = "$BASE/api/v1/prices?lat=51.1657&lon=10.4515&radius_km=450&currency=EUR&date__gte=$since&order_by=-date&size=100&page=$page"
            val response = try {
                http.get(url, mapOf("User-Agent" to USER_AGENT, "Accept" to "application/json"))
            } catch (e: Exception) {
                return FetchResult.Failure(FailureReason.NETWORK, e.message ?: "Netzwerkfehler", retryable = true)
            }
            when (response.code) {
                200 -> Unit
                403, 429 -> return FetchResult.Failure(FailureReason.BLOCKED, "HTTP ${response.code}", retryable = response.code == 429)
                else -> return FetchResult.Failure(FailureReason.HTTP, "HTTP ${response.code}", retryable = response.code >= 500)
            }
            val parsed = try {
                parsePage(response.body, context.nowMillis)
            } catch (e: Exception) {
                return FetchResult.Failure(FailureReason.PARSE, e.message ?: "Ungültige Antwort", retryable = false)
            }
            offers += parsed.offers
            rejected += parsed.rejected
            if (page >= parsed.pages) break
        }
        return FetchResult.Success(offers, context.nowMillis, rejected)
    }

    data class Page(val offers: List<Offer>, val rejected: Int, val pages: Int)

    fun parsePage(body: String, fetchedAt: Long): Page {
        val root = json.parseToJsonElement(body).jsonObject
        val items = root["items"] as? JsonArray ?: throw IllegalStateException("items fehlt")
        val pages = root.int("pages") ?: 1
        var rejected = 0
        val offers = items.mapNotNull { element ->
            val offer = (element as? JsonObject)?.let { toOffer(it, fetchedAt) }
            if (offer == null) rejected++
            offer
        }
        return Page(offers, rejected, pages)
    }

    /** Maps one price. Returns null for anything that cannot be shown truthfully. */
    internal fun toOffer(item: JsonObject, fetchedAt: Long): Offer? {
        if (item["duplicate_of"].let { it != null && it !is JsonNull }) return null
        if (item.str("currency") != "EUR") return null
        val priceValue = item.double("price") ?: return null
        if (priceValue <= 0.0 || priceValue > 1000.0) return null

        val location = item["location"] as? JsonObject ?: return null
        val retailer = retailerOf(location.str("osm_brand"), location.str("osm_name")) ?: return null
        val osmId = item.str("location_osm_id") ?: location.str("osm_id") ?: return null
        val store = Store(
            id = "osm:${item.str("location_osm_type") ?: "node"}:$osmId",
            retailer = retailer,
            name = location.str("osm_name") ?: retailer.displayName,
            street = listOfNotNull(location.str("osm_address_street"), location.str("osm_address_housenumber"))
                .joinToString(" ").ifBlank { null },
            postalCode = location.str("osm_address_postcode"),
            city = location.str("osm_address_city"),
        )
        if (!isGerman(location) || (store.postalCode.isNullOrBlank() && store.city.isNullOrBlank())) return null

        val product = item["product"] as? JsonObject
        val title = (item.str("product_name") ?: product?.str("product_name"))?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val brand = product?.str("brands")?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val quantityText = product?.str("quantity")
        val pricePer = item.str("price_per")
        val pack: PackSize? = when (pricePer) {
            "KILOGRAM" -> PackSize(1, 1.0, MeasureUnit.KILOGRAM)
            else -> PackSizeParser.parse(quantityText)
        }
        val date = item.str("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null

        val discounted = item["price_is_discounted"]?.let { (it as? JsonPrimitive)?.booleanOrNull } == true
        val without = item.double("price_without_discount")
        val reference = if (discounted && without != null && without > priceValue) {
            ReferencePrice(Cents.ofEuro(without), ReferencePriceKind.STRIKE_THROUGH)
        } else null

        val code = item.str("product_code") ?: product?.str("code")
        return Offer(
            id = "op-${item.str("id") ?: return null}",
            sourceId = descriptor.id,
            retailer = retailer,
            kind = PriceKind.OBSERVATION,
            title = title,
            brand = brand,
            category = categoryOf(product),
            sizeText = if (pricePer == "KILOGRAM") "Preis pro kg" else quantityText,
            packSize = pack,
            price = Cents.ofEuro(priceValue),
            referencePrice = reference,
            observedOn = date.toString(),
            availability = Coverage.STORE,
            stores = listOf(store),
            sourceUrl = item.str("location_id")?.let { "$BASE/locations/$it" } ?: BASE,
            ean = code?.takeIf { it.length in 8..14 && it.all(Char::isDigit) },
            fetchedAt = fetchedAt,
        )
    }

    /** OSM country is not always filled; a German postal code is accepted as evidence too. */
    private fun isGerman(location: JsonObject): Boolean {
        val country = location.str("osm_address_country")?.lowercase()
        if (country != null) return country == "deutschland" || country == "germany" || country == "de"
        return location.str("osm_address_postcode")?.let(de.kiezpreis.core.region.PostalCodes::isValid) == true
    }

    private fun categoryOf(product: JsonObject?): Category {
        val tags = (product?.get("categories_tags") as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
        fun has(vararg keys: String) = tags.any { t -> keys.any { t.contains(it) } }
        return when {
            has("beverages", "drinks", "sodas", "waters") -> Category.DRINKS
            has("dairies", "cheeses", "milks", "yogurts", "butters") -> Category.DAIRY
            has("meats", "sausages", "poultry") -> Category.MEAT
            has("fruits", "vegetables") -> Category.FRUIT_VEG
            has("breads", "pastries") -> Category.BAKERY
            has("snacks", "chocolates", "sweets", "confectioneries", "biscuits") -> Category.SWEETS
            has("frozen") -> Category.FROZEN
            has("dietary-supplements", "protein") -> Category.SPORTS_NUTRITION
            else -> Category.OTHER
        }
    }

    companion object {
        const val BASE = "https://prices.openfoodfacts.org"
        const val USER_AGENT = "Kiezpreis/0.3 (Android; supermarket price comparison; non-commercial)"
        val BERLIN_ZONE: ZoneId = ZoneId.of("Europe/Berlin")

        /** Maps OpenStreetMap brand/name to a chain. Plain "Netto" is ambiguous (two chains) and ignored. */
        fun retailerOf(brand: String?, name: String?): Retailer? {
            val text = listOfNotNull(brand, name).joinToString(" ").lowercase()
            return when {
                "aldi" in text && ("süd" in text || "sud" in text) -> Retailer.ALDI_SUED
                "aldi" in text && "nord" in text -> Retailer.ALDI_NORD
                // Plain "Aldi" could be either chain; without a reliable Nord/Süd border it is not assigned.
                "lidl" in text -> Retailer.LIDL
                "rewe" in text -> Retailer.REWE
                "edeka" in text -> Retailer.EDEKA
                "kaufland" in text -> Retailer.KAUFLAND
                "penny" in text -> Retailer.PENNY
                "netto marken" in text -> Retailer.NETTO_MD
                text.startsWith("dm") || " dm" in text -> Retailer.DM
                "rossmann" in text -> Retailer.ROSSMANN
                else -> null
            }
        }
    }
}

private fun JsonObject.str(key: String): String? = when (val v = this[key]) {
    null, is JsonNull -> null
    is JsonPrimitive -> v.content.takeIf { it.isNotBlank() && it != "null" }
    else -> null
}

private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.let { if (it is JsonNull) null else it.doubleOrNull }

private fun JsonObject.int(key: String): Int? = double(key)?.toInt()
