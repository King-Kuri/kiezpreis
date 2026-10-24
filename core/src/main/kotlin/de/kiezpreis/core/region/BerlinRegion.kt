package de.kiezpreis.core.region

import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Store

/**
 * Berlin is the only supported region. Region specific rules live here so a second region
 * could later be added as another implementation without touching the rest of the app.
 */
interface Region {
    val id: String
    val displayName: String
    fun containsPostalCode(postalCode: String): Boolean
}

object Berlin : Region {
    override val id = "berlin"
    override val displayName = "Berlin"

    /**
     * Berlin postal codes lie in 10115–14199, but those ranges also contain codes of
     * Brandenburg municipalities. Listed exclusions are Brandenburg codes inside the ranges.
     */
    private val ranges = listOf(10115..10999, 12043..12689, 13051..13629, 14050..14199)
    private val brandenburgExclusions = setOf(
        12529, // Schönefeld
    )

    override fun containsPostalCode(postalCode: String): Boolean {
        val code = postalCode.trim().takeIf { it.length == 5 && it.all(Char::isDigit) }?.toInt() ?: return false
        return ranges.any { code in it } && code !in brandenburgExclusions
    }

    fun isValidUserPostalCode(input: String): Boolean = containsPostalCode(input)

    /** A store counts as Berlin only with evidence: a Berlin postal code, or the city "Berlin" without a postal code. */
    fun isBerlinStore(store: Store): Boolean {
        val pc = store.postalCode
        if (!pc.isNullOrBlank()) return containsPostalCode(pc)
        return store.city?.trim()?.equals("Berlin", ignoreCase = true) == true
    }
}

object RegionalRules {

    /**
     * Classifies a store-bound price. Any store outside Berlin makes the price non-Berlin;
     * stores without location evidence make it unconfirmed.
     */
    fun classifyStores(stores: List<Store>): BerlinAvailability {
        if (stores.isEmpty()) return BerlinAvailability.UNCONFIRMED
        val berlin = stores.filter(Berlin::isBerlinStore)
        return when {
            berlin.isEmpty() && stores.any { hasLocation(it) } -> BerlinAvailability.OUTSIDE_BERLIN
            berlin.isEmpty() -> BerlinAvailability.UNCONFIRMED
            else -> BerlinAvailability.STORE_SPECIFIC
        }
    }

    private fun hasLocation(store: Store) = !store.postalCode.isNullOrBlank() || !store.city.isNullOrBlank()

    /** Whether an offer may appear in Berlin results at all. Unconfirmed ones are shown, but labelled. */
    fun isVisibleInBerlin(offer: Offer): Boolean = offer.availability != BerlinAvailability.OUTSIDE_BERLIN

    /** Offers that are confirmed for Berlin (used by strict filters and price alerts). */
    fun isConfirmedForBerlin(offer: Offer): Boolean = when (offer.availability) {
        BerlinAvailability.BERLIN_WIDE,
        BerlinAvailability.STORE_SPECIFIC,
        BerlinAvailability.NATIONAL_CONFIRMED -> true
        BerlinAvailability.UNCONFIRMED,
        BerlinAvailability.OUTSIDE_BERLIN -> false
    }

    /** Keeps only the Berlin stores of a store-specific offer, so a Brandenburg store never leaks into the UI. */
    fun berlinStoresOnly(offer: Offer): Offer =
        if (offer.availability == BerlinAvailability.STORE_SPECIFIC) offer.copy(stores = offer.stores.filter(Berlin::isBerlinStore))
        else offer
}
