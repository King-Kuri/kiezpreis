package de.kiezpreis.core

import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Store
import de.kiezpreis.core.region.Berlin
import de.kiezpreis.core.region.CoverageRules
import de.kiezpreis.core.region.PostalCodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegionTest {
    private fun store(pc: String?, city: String?) = Store("s-$pc-$city", Retailer.REWE, "REWE", postalCode = pc, city = city)
    private fun offer(coverage: Coverage, vararg stores: Store) =
        Offer("o", "t", Retailer.REWE, title = "X", price = null, availability = coverage, stores = stores.toList(), fetchedAt = 0)

    @Test fun `berlin postal codes`() {
        listOf("10115", "10178", "12209", "13591", "14057", "14109", "12689").forEach { assertTrue(Berlin.containsPostalCode(it), it) }
        listOf("15827", "14467", "12529", "14532", "60306", "80331", "1011", "abcde", "").forEach { assertFalse(Berlin.containsPostalCode(it), it) }
    }

    @Test fun `german postal codes and areas`() {
        listOf("01067", "10115", "80331", "99998").forEach { assertTrue(PostalCodes.isValid(it), it) }
        listOf("00999", "1011", "123456", "abcde", "", "99999").forEach { assertFalse(PostalCodes.isValid(it), it) }
        // Berlin is one area although its codes start with 10, 12, 13 and 14.
        assertEquals("berlin", PostalCodes.areaKey("10245"))
        assertEquals("berlin", PostalCodes.areaKey("13591"))
        // Potsdam shares the leading "14" but is its own area.
        assertEquals("14", PostalCodes.areaKey("14467"))
        assertEquals("80", PostalCodes.areaKey("80331"))
        assertNull(PostalCodes.areaKey("x"))
    }

    @Test fun `single stores never compete for best price`() {
        assertTrue(CoverageRules.isComparable(offer(Coverage.NATIONAL)))
        assertTrue(CoverageRules.isComparable(offer(Coverage.REGIONAL)))
        assertTrue(CoverageRules.isComparable(offer(Coverage.UNKNOWN)))
        assertFalse(CoverageRules.isComparable(offer(Coverage.STORE, store("10178", "Berlin"))))
    }

    @Test fun `nearby keeps stores of the same area only`() {
        val mixed = offer(Coverage.STORE, store("10243", "Berlin"), store("14467", "Potsdam"))
        assertTrue(CoverageRules.isNearby(mixed, "12051"))
        assertEquals(listOf("10243"), CoverageRules.nearbyStoresOnly(mixed, "12051").stores.map { it.postalCode })
        assertFalse(CoverageRules.isNearby(offer(Coverage.STORE, store("80331", "München")), "10245"))
        // City without postal code: "Berlin" still counts.
        assertTrue(CoverageRules.isNearby(offer(Coverage.STORE, store(null, "Berlin")), "10245"))
        // Unknown location cannot be placed and is left out of "my area".
        assertFalse(CoverageRules.isNearby(offer(Coverage.STORE, store(null, null)), "10245"))
        // Chain-wide offers are kept everywhere.
        assertTrue(CoverageRules.isNearby(offer(Coverage.NATIONAL), "80331"))
    }
}
