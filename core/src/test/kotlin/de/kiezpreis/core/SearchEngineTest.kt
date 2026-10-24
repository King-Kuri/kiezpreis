package de.kiezpreis.core

import de.kiezpreis.core.data.SampleOfferSource
import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.search.MatchQuality
import de.kiezpreis.core.search.SearchEngine
import de.kiezpreis.core.search.SearchFilters
import de.kiezpreis.core.search.SortOrder
import de.kiezpreis.core.search.TextNormalizer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchEngineTest {
    private val today = LocalDate.of(2026, 10, 22) // Wednesday
    private val engine = SearchEngine { today }
    private val offers = SampleOfferSource().build(today, 0)

    private fun titles(q: String, filters: SearchFilters = SearchFilters(), sort: SortOrder = SortOrder.RELEVANCE) =
        engine.search(q, offers, filters, sort).hits

    @Test fun `normalizer folds umlauts and hyphens`() {
        assertEquals(listOf("hahnchen", "brustfilet", "hahnchenbrustfilet"), TextNormalizer.tokens("Hähnchen-Brustfilet"))
        assertEquals(TextNormalizer.tokens("Haehnchen"), TextNormalizer.tokens("hähnchen"))
        assertTrue("cocacola" in TextNormalizer.tokens("Coca-Cola"))
    }

    @Test fun `monster ultra white keeps variants and other cities apart`() {
        val hits = titles("Monster Energy Ultra White")
        // Ultra Paradise is a different flavour and must not appear as an Ultra White result.
        assertTrue(hits.none { h -> h.group.offers.any { "Paradise" in it.title } })
        val all = hits.flatMap { it.group.offers }
        // The Potsdam store offer is outside Berlin and never shown.
        assertTrue(all.none { it.availability == BerlinAvailability.OUTSIDE_BERLIN })
        // Next week's ALDI offer is upcoming and shown; it is not expired.
        assertTrue(all.any { it.retailer == Retailer.ALDI_NORD })
        // Can and 4-pack share one product group, but the group knows sizes differ.
        assertEquals(1, hits.size)
        assertTrue(hits.single().group.hasMixedPackSizes)
    }

    @Test fun `cheapest comparable uses base price when sizes differ`() {
        val group = titles("Monster Ultra White").single().group
        // 4 x 0,5 l for 4,44 € = 2,22 €/l is cheaper than 0,99 € per 0,5 l (1,98 €/l)? No: 1,98 < 2,22.
        // Upcoming ALDI at 0,89 € (1,78 €/l) is the cheapest per liter.
        assertEquals(Retailer.ALDI_NORD, group.cheapestComparable()!!.retailer)
        // Among offers valid today, Lidl's can (1,98 €/l) beats the 4-pack (2,22 €/l).
        val active = group.cheapestComparable { de.kiezpreis.core.logic.ValidityRules.status(it, today) == de.kiezpreis.core.logic.ValidityStatus.ACTIVE }
        assertEquals(Retailer.LIDL, active!!.retailer)
    }

    @Test fun `typo tolerant and umlaut free search`() {
        assertTrue(titles("Hahnchenbrust").isNotEmpty())
        assertTrue(titles("Haehnchenbrustfilet").isNotEmpty())
        assertTrue(titles("Kerygold").isNotEmpty(), "one typo in an 8 letter word")
        assertTrue(titles("Monstr Ultra").isNotEmpty())
    }

    @Test fun `short words need exact matches`() {
        // "zero" must not match "hero" style typos: words up to 4 letters allow no edits.
        assertTrue(titles("Zera").isEmpty())
    }

    @Test fun `synonyms for protein powder and detergent`() {
        assertEquals(3, titles("Proteinpulver").size)
        assertTrue(titles("Waschmittel").size >= 3)
    }

    @Test fun `expired offers are excluded`() {
        val butter = titles("Kerrygold").flatMap { it.group.offers }
        assertTrue(butter.none { it.price == Cents(199) }, "last week's REWE offer is expired")
    }

    @Test fun `filters and empty states`() {
        val r = engine.search("Butter", offers, SearchFilters(retailers = setOf(Retailer.LIDL)))
        assertTrue(r.hits.isEmpty())
        assertTrue(r.unfilteredMatchCount > 0, "matches existed, filters removed them")
        val none = engine.search("Gummibärchen", offers)
        assertEquals(0, none.unfilteredMatchCount)
    }

    @Test fun `only confirmed berlin filter drops unconfirmed offers`() {
        val all = titles("Coca Cola Zero").flatMap { it.group.offers }
        val confirmed = titles("Coca Cola Zero", SearchFilters(onlyConfirmedBerlin = true)).flatMap { it.group.offers }
        assertTrue(all.any { it.availability == BerlinAvailability.UNCONFIRMED })
        assertTrue(confirmed.none { it.availability == BerlinAvailability.UNCONFIRMED })
    }

    @Test fun `sort by unit price puts unknown base prices last`() {
        val hits = titles("", SearchFilters(categories = setOf(Category.MEAT)), SortOrder.UNIT_PRICE)
        val last = hits.last().group
        assertEquals(null, last.bestUnitPrice())
    }

    @Test fun `price sort ascending`() {
        val prices = titles("", sort = SortOrder.PRICE).mapNotNull { it.group.lowestPrice()?.value }
        assertEquals(prices.sorted(), prices)
    }

    @Test fun `offer without price is listed but never as zero`() {
        val hit = titles("Bavaria Blu").single()
        assertEquals(null, hit.group.offers.single().price)
        assertEquals(null, hit.group.cheapestComparable())
    }

    @Test fun `exact matches rank before fuzzy`() {
        val list = listOf(
            offer("Butterkekse", "Leibniz"),
            offer("Buter Croissant", null),
        )
        val hits = engine.search("Butter", list).hits
        assertEquals(MatchQuality.EXACT, hits.first().quality)
    }

    @Test fun `suggestions`() {
        val s = engine.suggestions("ker", offers)
        assertTrue(s.contains("Kerrygold"))
    }

    private fun offer(title: String, brand: String?) = Offer(
        id = title, sourceId = "t", retailer = Retailer.REWE, title = title, brand = brand, price = Cents(100),
        availability = BerlinAvailability.BERLIN_WIDE, fetchedAt = 0,
    )
}
