package de.kiezpreis.core.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** Retail chains the app knows about. Knowing a chain does not mean a data source exists for it. */
@Serializable
enum class Retailer(val displayName: String, val shortName: String, val brandColor: Long, val website: String) {
    ALDI_NORD("ALDI Nord", "ALDI Nord", 0xFF00599C, "https://www.aldi-nord.de/angebote.html"),
    ALDI_SUED("ALDI Süd", "ALDI Süd", 0xFF0B2A6F, "https://www.aldi-sued.de/de/angebote.html"),
    LIDL("Lidl", "Lidl", 0xFF0050AA, "https://www.lidl.de/c/online-prospekte/s10005610"),
    REWE("REWE", "REWE", 0xFFCC071E, "https://www.rewe.de/angebote/"),
    EDEKA("EDEKA", "EDEKA", 0xFF1A4693, "https://www.edeka.de/angebote/"),
    KAUFLAND("Kaufland", "Kaufland", 0xFFE10915, "https://filiale.kaufland.de/angebote/"),
    PENNY("PENNY", "PENNY", 0xFFCD1414, "https://www.penny.de/angebote"),
    NETTO_MD("Netto Marken-Discount", "Netto", 0xFFFFD400, "https://www.netto-online.de/filialangebote"),
    DM("dm", "dm", 0xFF002878, "https://www.dm.de"),
    ROSSMANN("Rossmann", "Rossmann", 0xFFC3002D, "https://www.rossmann.de"),
    ;

    companion object {
        /** Supermarkets in scope for the MVP. Drugstores are prepared but not yet integrated. */
        val supermarkets = listOf(ALDI_NORD, ALDI_SUED, LIDL, REWE, EDEKA, KAUFLAND, PENNY, NETTO_MD)
    }
}

/** Amount of money in euro cents. Avoids floating point errors in price comparisons. */
@Serializable
@JvmInline
value class Cents(val value: Long) : Comparable<Cents> {
    override fun compareTo(other: Cents): Int = value.compareTo(other.value)
    operator fun minus(other: Cents) = Cents(value - other.value)

    /** German formatting, e.g. 1,29 €. */
    fun format(withSymbol: Boolean = true): String {
        val sign = if (value < 0) "-" else ""
        val abs = kotlin.math.abs(value)
        val text = "$sign${abs / 100},${(abs % 100).toString().padStart(2, '0')}"
        return if (withSymbol) "$text €" else text
    }

    companion object {
        fun ofEuro(euro: Double): Cents = Cents(Math.round(euro * 100))
    }
}

/** Measuring unit used for package sizes and base prices. */
@Serializable
enum class MeasureUnit(val symbol: String) {
    GRAM("g"), KILOGRAM("kg"), MILLILITER("ml"), LITER("l"), PIECE("Stück");

    /** The dimension used for comparisons: mass, volume or count. */
    val dimension: Dimension
        get() = when (this) {
            GRAM, KILOGRAM -> Dimension.MASS
            MILLILITER, LITER -> Dimension.VOLUME
            PIECE -> Dimension.COUNT
        }

    /** Factor to convert this unit into the dimension's smallest unit (g, ml, Stück). */
    val toBase: Double
        get() = when (this) {
            KILOGRAM, LITER -> 1000.0
            else -> 1.0
        }
}

@Serializable
enum class Dimension { MASS, VOLUME, COUNT }

/**
 * Package size as printed by the retailer, e.g. "6 x 0,33 l" -> count 6, amount 0.33, unit LITER.
 * Only created when the size could be read unambiguously.
 */
@Serializable
data class PackSize(
    val count: Int = 1,
    val amountPerItem: Double,
    val unit: MeasureUnit,
) {
    init {
        require(count >= 1) { "count must be >= 1" }
        require(amountPerItem > 0) { "amount must be > 0" }
    }

    val dimension: Dimension get() = unit.dimension

    /** Total content in g, ml or pieces. */
    val totalInBaseUnit: Double get() = count * amountPerItem * unit.toBase

    fun label(): String {
        val amount = formatAmount(amountPerItem)
        val single = if (unit == MeasureUnit.PIECE) "$amount ${unit.symbol}" else "$amount ${unit.symbol}"
        return if (count > 1) "$count × $single" else single
    }

    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace('.', ',')
}

/** How a reference price ("statt"-Preis) is evidenced by the source. */
@Serializable
enum class ReferencePriceKind(val label: String) {
    /** The retailer prints a crossed-out former price. */
    STRIKE_THROUGH("vorher"),
    /** Lowest price of the last 30 days, as required by § 11 PAngV. */
    LOWEST_30_DAYS("niedrigster Preis der letzten 30 Tage"),
    /** Manufacturer's recommended retail price. */
    RRP("UVP"),
}

@Serializable
data class ReferencePrice(val price: Cents, val kind: ReferencePriceKind)

/** What kind of data point this is. */
@Serializable
enum class PriceKind {
    /** A promotional offer with a validity period, published by the retailer. */
    OFFER,
    /** A price seen on a shelf or receipt at a given date (e.g. crowd-sourced). Not a promotion. */
    OBSERVATION,
}

/**
 * Where an offer applies. Stored separately from product information and shown with every price,
 * so the user can see whether an offer is local. Old names are still read from caches.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class Coverage {
    /** Valid in all stores of the chain in Germany, as stated by the source. */
    @JsonNames("NATIONAL_CONFIRMED")
    NATIONAL,
    /** Valid in a named area, e.g. the ALDI Nord region or one EDEKA region; see [Offer.area]. */
    @JsonNames("BERLIN_WIDE")
    REGIONAL,
    /** Only known for the stores listed in [Offer.stores]. */
    @JsonNames("STORE_SPECIFIC", "OUTSIDE_BERLIN")
    STORE,
    /** The source does not say where the offer applies. */
    @JsonNames("UNCONFIRMED")
    UNKNOWN,
}

@Serializable
data class Store(
    val id: String,
    val retailer: Retailer,
    val name: String,
    val street: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
)

/** Validity window. Dates are ISO strings (yyyy-MM-dd) to stay serialisation friendly. */
@Serializable
data class Validity(val from: String? = null, val to: String? = null)

/** Special conditions that make the price unavailable to everybody. */
@Serializable
enum class OfferCondition(val label: String) {
    APP_OR_CARD_ONLY("nur mit App / Kundenkarte"),
    COUPON("nur mit Coupon"),
    MULTI_BUY("Mehrfachkauf nötig"),
    WHILE_STOCKS_LAST("solange Vorrat reicht"),
    PARTICIPATING_STORES("nur in teilnehmenden Filialen"),
    PLUS_DEPOSIT("zzgl. Pfand"),
}

/** Single price point for a product at a retailer. */
@Serializable
data class Offer(
    val id: String,
    val sourceId: String,
    val retailer: Retailer,
    val kind: PriceKind = PriceKind.OFFER,
    val title: String,
    val brand: String? = null,
    val category: Category = Category.OTHER,
    /** Raw size text exactly as given by the source, e.g. "je 0,5-l-Dose". */
    val sizeText: String? = null,
    /** Parsed size; null when the source text was missing or ambiguous. */
    val packSize: PackSize? = null,
    /** Current price; null when the source lists the product without a price. */
    val price: Cents?,
    val referencePrice: ReferencePrice? = null,
    /** Base price as printed by the retailer (kept for transparency, never parsed into facts silently). */
    val statedBasePrice: String? = null,
    val validity: Validity = Validity(),
    /** For [PriceKind.OBSERVATION]: the day the price was seen. */
    val observedOn: String? = null,
    val availability: Coverage,
    /** For [Coverage.REGIONAL]: the area as the source names it, e.g. "ALDI-Nord-Gebiet". */
    val area: String? = null,
    val stores: List<Store> = emptyList(),
    val conditions: List<OfferCondition> = emptyList(),
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val ean: String? = null,
    /** Epoch millis when the source was fetched. */
    val fetchedAt: Long,
    /** True for built-in demonstration data. Never presented as a real market price. */
    val isSample: Boolean = false,
)

@Serializable
enum class Category(val label: String) {
    DRINKS("Getränke"),
    DAIRY("Milchprodukte"),
    MEAT("Fleisch & Wurst"),
    FRUIT_VEG("Obst & Gemüse"),
    BAKERY("Brot & Backwaren"),
    PANTRY("Vorrat"),
    SWEETS("Süßes & Snacks"),
    FROZEN("Tiefkühl"),
    HOUSEHOLD("Haushalt"),
    CARE("Drogerie & Pflege"),
    SPORTS_NUTRITION("Sportnahrung"),
    OTHER("Sonstiges"),
}
