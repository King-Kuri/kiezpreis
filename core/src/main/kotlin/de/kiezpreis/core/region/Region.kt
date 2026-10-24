package de.kiezpreis.core.region

import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Store

/** German postal codes and the "Gegend" used by the "Nur meine Gegend" filter. */
object PostalCodes {

    /** Five digits in the range German postal codes use. */
    fun isValid(code: String): Boolean {
        val c = code.trim()
        return c.length == 5 && c.all(Char::isDigit) && c.toInt() in 1001..99998
    }

    /**
     * The area a postal code belongs to: Berlin as a whole (its codes span several leading digits),
     * otherwise the two-digit postal region, e.g. "80" for Munich. Null for invalid input.
     */
    fun areaKey(code: String): String? = when {
        !isValid(code) -> null
        Berlin.containsPostalCode(code) -> "berlin"
        else -> code.trim().take(2)
    }

    fun areaLabel(code: String): String = when (val key = areaKey(code)) {
        null -> code
        "berlin" -> "Berlin"
        else -> "PLZ-Gebiet $key…"
    }
}

object Berlin {
    /**
     * Berlin postal codes lie in 10115–14199, but those ranges also contain codes of
     * Brandenburg municipalities. Listed exclusions are Brandenburg codes inside the ranges.
     */
    private val ranges = listOf(10115..10999, 12043..12689, 13051..13629, 14050..14199)
    private val brandenburgExclusions = setOf(
        12529, // Schönefeld
    )

    fun containsPostalCode(postalCode: String): Boolean {
        val code = postalCode.trim().takeIf { it.length == 5 && it.all(Char::isDigit) }?.toInt() ?: return false
        return ranges.any { code in it } && code !in brandenburgExclusions
    }
}

object CoverageRules {

    /**
     * Whether an offer may compete for "best price". Prices of single stores are shown in their own
     * section instead, so a shelf price in another city never wins against a chain-wide offer.
     */
    fun isComparable(offer: Offer): Boolean = offer.availability != Coverage.STORE

    /** Area key of a store, or null when the source gives no usable location. */
    fun storeArea(store: Store): String? {
        store.postalCode?.let { PostalCodes.areaKey(it) }?.let { return it }
        return if (store.city?.trim()?.equals("Berlin", ignoreCase = true) == true) "berlin" else null
    }

    /**
     * "Nur meine Gegend": store prices count only for stores in the user's area. Chain-wide,
     * regional and unknown offers stay, because the source does not exclude the user's area;
     * their label says how far they are confirmed.
     */
    fun isNearby(offer: Offer, homePostalCode: String): Boolean {
        val home = PostalCodes.areaKey(homePostalCode) ?: return true
        return when (offer.availability) {
            Coverage.STORE -> offer.stores.any { storeArea(it) == home }
            else -> true
        }
    }

    /** For store prices under "Nur meine Gegend": only the stores in the user's area. */
    fun nearbyStoresOnly(offer: Offer, homePostalCode: String): Offer {
        val home = PostalCodes.areaKey(homePostalCode) ?: return offer
        return if (offer.availability == Coverage.STORE) offer.copy(stores = offer.stores.filter { storeArea(it) == home }) else offer
    }
}
