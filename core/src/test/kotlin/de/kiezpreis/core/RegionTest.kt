package de.kiezpreis.core

import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Store
import de.kiezpreis.core.region.Berlin
import de.kiezpreis.core.region.RegionalRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionTest {
    private fun store(pc: String?, city: String?) = Store("s", Retailer.REWE, "REWE", postalCode = pc, city = city)

    @Test fun `berlin postal codes`() {
        listOf("10115", "10178", "12209", "13591", "14057", "14109", "12689").forEach { assertTrue(Berlin.containsPostalCode(it), it) }
    }

    @Test fun `brandenburg and other cities are not berlin`() {
        listOf("15827", "14467", "12529", "14532", "16548", "60306", "80331", "1011", "abcde", "").forEach {
            assertFalse(Berlin.containsPostalCode(it), it)
        }
    }

    @Test fun `store classification`() {
        assertEquals(BerlinAvailability.STORE_SPECIFIC, RegionalRules.classifyStores(listOf(store("10178", "Berlin"))))
        // Dahlewitz lies inside a 25 km circle around Berlin but is in Brandenburg (seen in live Open Prices data).
        assertEquals(BerlinAvailability.OUTSIDE_BERLIN, RegionalRules.classifyStores(listOf(store("15827", "Dahlewitz"))))
        // A postal code wins over a wrong city label.
        assertEquals(BerlinAvailability.OUTSIDE_BERLIN, RegionalRules.classifyStores(listOf(store("14467", "Berlin"))))
        assertEquals(BerlinAvailability.STORE_SPECIFIC, RegionalRules.classifyStores(listOf(store(null, "Berlin"))))
        assertEquals(BerlinAvailability.UNCONFIRMED, RegionalRules.classifyStores(listOf(store(null, null))))
        assertEquals(BerlinAvailability.UNCONFIRMED, RegionalRules.classifyStores(emptyList()))
        // Mixed: Berlin store present -> store specific, but only Berlin stores are kept for display.
        assertEquals(BerlinAvailability.STORE_SPECIFIC, RegionalRules.classifyStores(listOf(store("10243", "Berlin"), store("14467", "Potsdam"))))
    }
}
