package de.kiezpreis.core

import de.kiezpreis.core.data.FailureReason
import de.kiezpreis.core.data.FetchContext
import de.kiezpreis.core.data.FetchResult
import de.kiezpreis.core.data.HttpClient
import de.kiezpreis.core.data.HttpResponse
import de.kiezpreis.core.data.OpenPricesSource
import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.PriceKind
import de.kiezpreis.core.model.Retailer
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenPricesSourceTest {
    private val fixture = javaClass.getResource("/open_prices_page.json")!!.readText()

    private class FakeHttp(val respond: (String) -> HttpResponse) : HttpClient {
        val urls = mutableListOf<String>()
        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            require(headers["User-Agent"]?.startsWith("Kiezpreis") == true) { "custom User-Agent required by Open Food Facts" }
            return respond(url)
        }
    }

    @Test fun `parses berlin prices and drops everything else`() = runTest {
        val source = OpenPricesSource(FakeHttp { HttpResponse(200, fixture) })
        val result = source.fetch(FetchContext(nowMillis = 1_793_000_000_000, postalCode = null))
        assertIs<FetchResult.Success>(result)
        val byId = result.offers.associateBy { it.id }
        // Kept: REWE Berlin, Kaufland Berlin (discounted), Lidl Berlin bananas (per kg).
        assertEquals(setOf("op-101", "op-103", "op-104"), byId.keys)
        // Dropped: Dahlewitz (Brandenburg), empty name, CHF, duplicate, ambiguous "Netto".
        assertEquals(5, result.rejected)

        val cola = byId.getValue("op-101")
        assertEquals(Retailer.REWE, cola.retailer)
        assertEquals(PriceKind.OBSERVATION, cola.kind)
        assertEquals(BerlinAvailability.STORE_SPECIFIC, cola.availability)
        assertEquals("12209", cola.stores.single().postalCode)
        assertEquals(Cents(139), cola.price)
        assertEquals(500.0, cola.packSize!!.totalInBaseUnit)
        assertEquals("2026-10-09", cola.observedOn)
        assertEquals("5000112637939", cola.ean)

        val burrito = byId.getValue("op-103")
        assertEquals(Cents(299), burrito.referencePrice!!.price)

        val bananas = byId.getValue("op-104")
        assertEquals("Preis pro kg", bananas.sizeText)
        assertEquals(1000.0, bananas.packSize!!.totalInBaseUnit)
        assertTrue(result.offers.none { it.isSample })
    }

    @Test fun `network errors become failures without data`() = runTest {
        val source = OpenPricesSource(object : HttpClient {
            override suspend fun get(url: String, headers: Map<String, String>): HttpResponse = throw IOException("offline")
        })
        val result = source.fetch(FetchContext(0, null))
        assertIs<FetchResult.Failure>(result)
        assertEquals(FailureReason.NETWORK, result.reason)
        assertTrue(result.retryable)
    }

    @Test fun `http and parse errors`() = runTest {
        assertEquals(FailureReason.HTTP, (OpenPricesSource(FakeHttp { HttpResponse(503, "") }).fetch(FetchContext(0, null)) as FetchResult.Failure).reason)
        assertEquals(FailureReason.BLOCKED, (OpenPricesSource(FakeHttp { HttpResponse(429, "") }).fetch(FetchContext(0, null)) as FetchResult.Failure).reason)
        assertEquals(FailureReason.PARSE, (OpenPricesSource(FakeHttp { HttpResponse(200, "<html>") }).fetch(FetchContext(0, null)) as FetchResult.Failure).reason)
        assertEquals(FailureReason.PARSE, (OpenPricesSource(FakeHttp { HttpResponse(200, "{\"foo\":1}") }).fetch(FetchContext(0, null)) as FetchResult.Failure).reason)
    }

    @Test fun `query is limited to a circle around berlin and recent dates`() = runTest {
        val http = FakeHttp { HttpResponse(200, fixture) }
        // 2026-10-24 12:00 UTC (1792843200000)
        OpenPricesSource(http).fetch(FetchContext(1792843200000L, null))
        val url = http.urls.single()
        assertTrue("lat=52.5200" in url && "lon=13.4050" in url && "radius_km=25" in url)
        assertTrue("date__gte=2026-10-03" in url, url)
    }
}
