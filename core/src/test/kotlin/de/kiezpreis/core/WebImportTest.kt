package de.kiezpreis.core

import de.kiezpreis.core.data.FetchContext
import de.kiezpreis.core.data.FetchResult
import de.kiezpreis.core.data.JsonFileStore
import de.kiezpreis.core.importer.CapturedPage
import de.kiezpreis.core.importer.CapturedResponse
import de.kiezpreis.core.importer.Categorizer
import de.kiezpreis.core.importer.DomCard
import de.kiezpreis.core.importer.Evidence
import de.kiezpreis.core.importer.MarketChoice
import de.kiezpreis.core.importer.OfferExtractor
import de.kiezpreis.core.importer.PriceText
import de.kiezpreis.core.importer.WebImport
import de.kiezpreis.core.importer.WebImportSource
import de.kiezpreis.core.importer.WebImportStore
import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.OfferCondition
import de.kiezpreis.core.model.ReferencePriceKind
import de.kiezpreis.core.model.Retailer
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebImportTest {

    private val now = 1792843200000L // 2026-10-24 12:00 UTC

    private fun page(
        url: String = "https://www.rewe.de/angebote/berlin/123/rewe-markt/",
        jsonLd: List<String> = emptyList(),
        embedded: List<String> = emptyList(),
        responses: List<String> = emptyList(),
        cards: List<DomCard> = emptyList(),
    ) = CapturedPage(url, "Angebote", jsonLd, embedded, responses.map { CapturedResponse("$url/api", it) }, cards)

    @Test
    fun priceTextAcceptsGermanFormatsAndRejectsAmbiguity() {
        assertEquals(Cents(199), PriceText.parse("1,99 €"))
        assertEquals(Cents(199), PriceText.parse("1.99*"))
        assertEquals(Cents(99), PriceText.parse("-,99"))
        assertEquals(Cents(200), PriceText.parse("2,-"))
        assertEquals(Cents(1299), PriceText.parse("€ 12,99"))
        assertNull(PriceText.parse("199"), "bare number is not a price")
        assertNull(PriceText.parse("1,99 statt 2,49"), "two prices are ambiguous")
        assertNull(PriceText.parse("gültig ab 24.10.2026"), "a date is no price")
        assertNull(PriceText.parse("-,-"))
    }

    @Test
    fun readsSchemaOrgProduct() {
        val ld = """
            {"@context":"https://schema.org","@graph":[{"@type":"Product","name":"Milka Alpenmilch 100 g",
             "brand":{"@type":"Brand","name":"Milka"},"gtin13":"7622300336738","image":"https://img.example/milka.jpg",
             "offers":{"@type":"Offer","price":"0.99","priceCurrency":"EUR","validFrom":"2026-10-20","priceValidUntil":"2026-10-25"}}]}
        """.trimIndent()
        val items = OfferExtractor.extract(page(jsonLd = listOf(ld)))
        assertEquals(1, items.size)
        val m = items.single()
        assertEquals("Milka Alpenmilch 100 g", m.title)
        assertEquals("Milka", m.brand)
        assertEquals(Cents(99), m.price)
        assertEquals("2026-10-20", m.validFrom)
        assertEquals("2026-10-25", m.validTo)
        assertEquals("7622300336738", m.ean)
        assertEquals(Evidence.STRUCTURED, m.evidence)
    }

    @Test
    fun readsNestedPageDataWithCentsAndReferencePrices() {
        val next = """
            {"props":{"pageProps":{"offers":[
              {"title":"Coca-Cola 1,25 l","priceInCents":119,"oldPriceInCents":179,"grammage":"1 l = 0,95 €",
               "validity":{"from":1792368000000,"to":"2026-10-25T21:59:59Z"},"badge":"zzgl. Pfand"},
              {"productName":"Bio Bananen","prices":{"current":"1,49 €","lowestPrice30Days":"1,79 €"},"subtitle":"1 kg"},
              {"name":"Red Bull 0,25 l","price":{"value":0.99,"currency":"EUR"},"regularPrice":1.49,"info":"Nur mit der App"}
            ]}}}
        """.trimIndent()
        val items = OfferExtractor.extract(page(embedded = listOf(next))).associateBy { it.title }
        assertEquals(3, items.size)
        val cola = items.getValue("Coca-Cola 1,25 l")
        assertEquals(Cents(119), cola.price)
        assertEquals(Cents(179), cola.referencePrice?.price)
        assertEquals("1 l = 0,95 €", cola.basePriceText)
        assertEquals("2026-10-19", cola.validFrom)
        assertEquals("2026-10-25", cola.validTo)
        assertTrue(OfferCondition.PLUS_DEPOSIT in cola.conditions)
        val banana = items.getValue("Bio Bananen")
        assertEquals(Cents(149), banana.price)
        assertEquals(ReferencePriceKind.LOWEST_30_DAYS, banana.referencePrice?.kind)
        assertEquals("1 kg", banana.sizeText)
        val bull = items.getValue("Red Bull 0,25 l")
        assertEquals(Cents(99), bull.price)
        assertTrue(OfferCondition.APP_OR_CARD_ONLY in bull.conditions)
    }

    @Test
    fun dropsAmbiguousOrForeignPrices() {
        val json = """
            [{"name":"Ganzzahl","price":199},
             {"name":"Franken","price":"2.50","currency":"CHF"},
             {"name":"Zu teuer","price":"1500.00"},
             {"name":"Kein Preis","price":null},
             {"name":"Filter 0-5 €","price":"0-5"},
             {"name":"Altpreis nicht höher","price":"1,99","oldPrice":"1,99"}]
        """.trimIndent()
        val items = OfferExtractor.extract(page(responses = listOf(json)))
        assertEquals(listOf("Altpreis nicht höher"), items.map { it.title })
        assertNull(items.single().referencePrice)
    }

    @Test
    fun domCardsNeedOneClearPrice() {
        val cards = listOf(
            DomCard("Butter 250 g", "1,79 €", oldPrice = "2,29 €", basePrice = "1 kg = 7,16 €"),
            DomCard("Zwei Preise", "1,79 € 2,29 €"),
            DomCard("Nur Zahl", "179"),
            DomCard("Mit UVP", "4,99", oldPrice = "UVP 6,99"),
        )
        val items = OfferExtractor.extract(page(cards = cards)).associateBy { it.title }
        assertEquals(setOf("Butter 250 g", "Mit UVP"), items.keys)
        assertEquals(Cents(229), items.getValue("Butter 250 g").referencePrice?.price)
        assertEquals(ReferencePriceKind.RRP, items.getValue("Mit UVP").referencePrice?.kind)
        assertEquals(Evidence.PAGE_TEXT, items.getValue("Butter 250 g").evidence)
    }

    @Test
    fun betterEvidenceWinsOnDuplicates() {
        val ld = """{"@type":"Product","name":"Butter 250 g","offers":{"price":"1.79","priceCurrency":"EUR"}}"""
        val items = OfferExtractor.extract(page(jsonLd = listOf(ld), cards = listOf(DomCard("Butter 250 g", "1,79 €"))))
        assertEquals(1, items.size)
        assertEquals(Evidence.STRUCTURED, items.single().evidence)
    }

    @Test
    fun retailerHostsAreMatchedExactly() {
        assertEquals(Retailer.REWE, WebImport.retailerForHost("www.rewe.de"))
        assertEquals(Retailer.KAUFLAND, WebImport.retailerForHost("filiale.kaufland.de"))
        assertEquals(Retailer.NETTO_MD, WebImport.retailerForHost("www.netto-online.de"))
        assertNull(WebImport.retailerForHost("www.aldi-sued.de"), "ALDI Süd does not serve Berlin")
        assertNull(WebImport.retailerForHost("rewe.de.evil.example"))
        assertNull(WebImport.retailerForHost("notrewe.de"))
    }

    @Test
    fun berlinMarketMakesOffersStoreSpecific() {
        val p = page(embedded = listOf("""{"market":{"name":"REWE Heerstr.","zipCode":"13591"},"items":[{"name":"Butter","price":"1,79"}]}"""))
        val draft = WebImport.draft(p, now)
        assertEquals(listOf("13591"), draft.detectedPostalCodes)
        val batch = WebImport.toBatch(draft, draft.items, MarketChoice.BerlinMarket("13591", "REWE Heerstr."), now).getOrThrow()
        val offer = batch.offers.single()
        assertEquals(BerlinAvailability.STORE_SPECIFIC, offer.availability)
        assertEquals("13591", offer.stores.single().postalCode)
        assertEquals(WebImport.SOURCE_ID, offer.sourceId)
        assertEquals(false, offer.isSample)

        val unknown = WebImport.toBatch(draft, draft.items, MarketChoice.Unknown, now).getOrThrow()
        assertEquals(BerlinAvailability.UNCONFIRMED, unknown.offers.single().availability)

        assertTrue(WebImport.toBatch(draft, draft.items, MarketChoice.BerlinMarket("14467", null), now).isFailure, "Potsdam is not Berlin")
    }

    @Test
    fun pagesOfOtherSitesCannotBeImported() {
        val draft = WebImport.draft(page(url = "https://www.aldi-sued.de/angebote", responses = listOf("""[{"name":"X","price":"1,00"}]""")), now)
        assertNull(draft.retailer)
        assertTrue(WebImport.toBatch(draft, draft.items, MarketChoice.Unknown, now).isFailure)
    }

    @Test
    fun undatedImportsExpireAfterAWeek() = runTest {
        val dir = Files.createTempDirectory("imports").toFile()
        val store = WebImportStore(JsonFileStore(dir))
        val p = page(responses = listOf("""[{"name":"Ohne Datum","price":"1,00"},{"name":"Mit Datum","price":"2,00","validTo":"2026-11-30"}]"""))
        val draft = WebImport.draft(p, now)
        store.save(WebImport.toBatch(draft, draft.items, MarketChoice.Unknown, now).getOrThrow())

        val source = WebImportSource(store)
        val fresh = source.fetch(FetchContext(now + 86_400_000L, null)) as FetchResult.Success
        assertEquals(2, fresh.offers.size)
        val old = source.fetch(FetchContext(now + 8 * 86_400_000L, null)) as FetchResult.Success
        assertEquals(listOf("Mit Datum"), old.offers.map { it.title })

        // A new import for the same retailer and market replaces the old one.
        store.save(WebImport.toBatch(draft, draft.items.take(1), MarketChoice.Unknown, now).getOrThrow())
        assertEquals(1, WebImportStore(JsonFileStore(dir)).batches.value.single().offers.size)
    }

    @Test
    fun categoriesFromCompoundWords() {
        assertEquals(Category.DAIRY, Categorizer.guess("Weihenstephan Vollmilch 1 l"))
        assertEquals(Category.MEAT, Categorizer.guess("Hähnchenbrustfilet 400 g"))
        assertEquals(Category.MEAT, Categorizer.guess("Schweinenacken"))
        assertEquals(Category.PANTRY, Categorizer.guess("Basmati Reis"))
        assertEquals(Category.OTHER, Categorizer.guess("Gartenstuhl"))
    }
}
