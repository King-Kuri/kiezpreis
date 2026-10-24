package de.kiezpreis.core.search

import de.kiezpreis.core.logic.PriceMath
import de.kiezpreis.core.logic.UnitPrice
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PackSize
import de.kiezpreis.core.region.CoverageRules

/**
 * Decides which offers describe the same product. Two offers belong together only when their
 * brand and all significant name words match: "Ultra White" and "Ultra Paradise" stay apart.
 * Different package sizes of the same product share a group but are compared via base price only.
 */
object ProductIdentity {

    /** Words that describe packaging or marketing and do not distinguish products. */
    private val noise = setOf(
        "je", "dose", "dosen", "flasche", "fl", "pckg", "packung", "pack", "becher", "beutel", "glas",
        "stuck", "stk", "kasten", "tray", "zzgl", "pfand", "einweg", "mehrweg", "drink", "getrank",
        "sorten", "versch", "verschiedene", "oder", "und", "mit", "der", "die", "das", "aus", "von",
        "energy", "energydrink", "aktion", "neu", "original", "classic",
    )
    private val sizeToken = Regex("^\\d+([.,]\\d+)?(g|kg|ml|l|cl|x)?$")
    private val unitOnly = setOf("g", "kg", "ml", "l", "cl", "x")

    /** True for packaging and marketing words that a query may contain without the title needing them. */
    fun isGeneric(word: String): Boolean = word in noise

    data class Key(val brand: String, val words: Set<String>) {
        val id: String get() = (listOf(brand) + words.sorted()).joinToString("|")
    }

    fun key(offer: Offer): Key {
        offer.ean?.takeIf { it.isNotBlank() }?.let { return Key("ean", setOf(it)) }
        val brandTokens = offer.brand?.let { TextNormalizer.tokens(it) }.orEmpty()
        val brand = brandTokens.joinToString(" ")
        val words = TextNormalizer.tokens(offer.title)
            .filterNot { it in noise || it in unitOnly || sizeToken.matches(it) || it in brandTokens }
            .toSet()
        return Key(brand, words)
    }
}

/** Offers that describe one product (possibly in different package sizes). */
data class ProductGroup(
    val key: String,
    val title: String,
    val brand: String?,
    val offers: List<Offer>,
) {
    val packSizes: List<PackSize?> get() = offers.map { it.packSize }.distinct()
    val retailers get() = offers.map { it.retailer }.distinct()
    val hasMixedPackSizes: Boolean get() = packSizes.size > 1

    /** Prices that apply chain-wide, regionally or without stated limits. */
    val comparableOffers: List<Offer> get() = offers.filter(CoverageRules::isComparable)

    /** Prices of single stores, shown separately with their location. */
    val storeOffers: List<Offer> get() = offers.filterNot(CoverageRules::isComparable)

    /**
     * The cheapest offer when a fair comparison is possible. Only [comparableOffers] compete,
     * so a single store's shelf price somewhere else never becomes "the best price":
     * - same package size everywhere -> lowest price;
     * - different sizes -> lowest base price, only if every offer has one of the same dimension.
     * Otherwise null: the UI then does not crown a winner.
     */
    fun cheapestComparable(eligible: (Offer) -> Boolean = { true }): Offer? {
        val candidates = comparableOffers.filter(eligible)
        val priced = candidates.filter { it.price != null }
        if (priced.isEmpty()) return null
        if (candidates.map { it.packSize }.distinct().size == 1) return priced.minByOrNull { it.price!!.value }
        val withUnit = priced.map { it to PriceMath.unitPrice(it) }
        if (withUnit.any { it.second == null } || priced.size != candidates.size) return null
        val dims = withUnit.map { it.second!!.dimension }.distinct()
        if (dims.size != 1) return null
        return withUnit.minByOrNull { it.second!!.cents.value }!!.first
    }

    fun bestUnitPrice(): UnitPrice? = comparableOffers.ifEmpty { offers }.mapNotNull { PriceMath.unitPrice(it) }
        .groupBy { it.dimension }.values.singleOrNull()?.minByOrNull { it.cents.value }

    /** Lowest comparable price; for products known only from single stores, their lowest price. */
    fun lowestPrice() = (comparableOffers.mapNotNull { it.price }.ifEmpty { offers.mapNotNull { it.price } }).minByOrNull { it.value }
}

object Grouping {
    fun group(offers: List<Offer>): List<ProductGroup> =
        offers.groupBy { ProductIdentity.key(it).id }.map { (key, list) ->
            // The most common title wins; on a tie the shorter, plainer one.
            val representative = list.groupBy { it.title }.entries
                .sortedWith(compareByDescending<Map.Entry<String, List<Offer>>> { it.value.size }.thenBy { it.key.length })
                .first().value.first()
            ProductGroup(key = key, title = representative.title, brand = list.firstNotNullOfOrNull { it.brand }, offers = list)
        }
}
