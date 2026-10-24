package de.kiezpreis.core

import de.kiezpreis.core.data.FailureReason
import de.kiezpreis.core.data.FetchContext
import de.kiezpreis.core.data.FetchResult
import de.kiezpreis.core.data.Favorite
import de.kiezpreis.core.data.JsonFileStore
import de.kiezpreis.core.data.OfferRepository
import de.kiezpreis.core.data.OfferSource
import de.kiezpreis.core.data.SampleOfferSource
import de.kiezpreis.core.data.SourceCatalog
import de.kiezpreis.core.data.SourceStatus
import de.kiezpreis.core.data.UserDataRepository
import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.ReferencePrice
import de.kiezpreis.core.model.ReferencePriceKind
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Validity
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryTest {
    private val dir = Files.createTempDirectory("kiez").toFile()

    private class ScriptedSource(var next: () -> FetchResult) : OfferSource {
        var calls = 0
        override val descriptor = SourceCatalog.openPrices
        override suspend fun fetch(context: FetchContext): FetchResult { calls++; return next() }
    }

    private fun offer(id: String, price: Long?, ref: Long? = null, availability: Coverage = Coverage.STORE) = Offer(
        id = id, sourceId = "open_prices", retailer = Retailer.REWE, title = "Butter $id", price = price?.let(::Cents),
        referencePrice = ref?.let { ReferencePrice(Cents(it), ReferencePriceKind.STRIKE_THROUGH) },
        availability = availability, fetchedAt = 1,
    )

    @Test fun `failed source keeps cached data, invents nothing, survives restart`() = runTest {
        var now = 1_000L
        val source = ScriptedSource { FetchResult.Success(listOf(offer("a", 199), offer("b", 0), offer("c", 150, ref = 120)), now) }
        val repo = OfferRepository(listOf(source), JsonFileStore(dir), clock = { now }, retryDelayMillis = 1)
        repo.setEnabledSources(setOf("open_prices"))
        repo.refresh(force = true)
        val first = repo.state.value
        // Zero price is invalid and dropped; implausible reference price is removed, offer kept.
        assertEquals(listOf("a", "c"), first.offers.map { it.id })
        assertNull(first.offers.last().referencePrice)
        assertEquals(1, (first.sources.single().status as SourceStatus.Ok).rejected)

        source.next = { FetchResult.Failure(FailureReason.NETWORK, "offline", retryable = true) }
        source.calls = 0
        now = 2_000L
        repo.refresh(force = true)
        val second = repo.state.value
        assertEquals(3, source.calls, "bounded retries")
        assertEquals(listOf("a", "c"), second.offers.map { it.id }, "cache kept")
        assertIs<SourceStatus.Failed>(second.sources.single().status)
        assertEquals(1_000L, second.sources.single().dataFetchedAt)

        // New process: cache loaded from disk.
        val restarted = OfferRepository(listOf(source), JsonFileStore(dir), clock = { now })
        restarted.setEnabledSources(setOf("open_prices"))
        assertEquals(listOf("a", "c"), restarted.state.value.offers.map { it.id })
    }

    @Test fun `non retryable failure is not retried and disabled sources contribute nothing`() = runTest {
        val source = ScriptedSource { FetchResult.Failure(FailureReason.PARSE, "kaputt", retryable = false) }
        val repo = OfferRepository(listOf(source), JsonFileStore(dir), retryDelayMillis = 1)
        repo.setEnabledSources(setOf("open_prices"))
        repo.refresh(force = true)
        assertEquals(1, source.calls)
        assertFalse(repo.state.value.hasAnyData)
        repo.setEnabledSources(emptySet())
        assertIs<SourceStatus.Disabled>(repo.state.value.sources.single().status)
    }

    @Test fun `caches from the berlin-only version are still read`() = runTest {
        java.io.File(dir, "cache_open_prices.json").writeText(
            """{"fetchedAt":1,"offers":[
              {"id":"a","sourceId":"open_prices","retailer":"REWE","title":"A","price":100,"availability":"STORE_SPECIFIC","fetchedAt":1},
              {"id":"b","sourceId":"open_prices","retailer":"REWE","title":"B","price":100,"availability":"OUTSIDE_BERLIN","fetchedAt":1},
              {"id":"c","sourceId":"open_prices","retailer":"REWE","title":"C","price":100,"availability":"NATIONAL_CONFIRMED","fetchedAt":1},
              {"id":"d","sourceId":"open_prices","retailer":"REWE","title":"D","price":100,"availability":"UNCONFIRMED","fetchedAt":1}]}""",
        )
        val repo = OfferRepository(listOf(ScriptedSource { error("not called") }), JsonFileStore(dir), clock = { 2 })
        repo.setEnabledSources(setOf("open_prices"))
        assertEquals(
            listOf(Coverage.STORE, Coverage.STORE, Coverage.NATIONAL, Coverage.UNKNOWN),
            repo.state.value.offers.map { it.availability },
        )
    }

    @Test fun `sample data is flagged as such`() = runTest {
        val repo = OfferRepository(listOf(SampleOfferSource()), JsonFileStore(dir))
        repo.setEnabledSources(setOf("sample"))
        repo.refresh(force = true)
        assertTrue(repo.state.value.onlySampleData)
        assertTrue(repo.state.value.offers.all { it.isSample && it.sourceUrl == null })
    }

    @Test fun `user data persists across restarts`() {
        val store = JsonFileStore(dir)
        val users = UserDataRepository(store)
        users.toggleFavorite(Favorite("k1", "Kerrygold Butter", "Kerrygold", "Kerrygold", 1))
        users.addRecentSearch("Monster Ultra White")
        assertFalse(users.setPostalCode("1446"), "four digits are rejected")
        assertTrue(users.setPostalCode("14467"), "any German postal code is accepted")
        assertTrue(users.setPostalCode("10245"))
        val reloaded = UserDataRepository(JsonFileStore(dir))
        assertEquals("k1", reloaded.favorites.value.single().productKey)
        assertEquals(listOf("Monster Ultra White"), reloaded.recentSearches.value)
        assertEquals("10245", reloaded.prefs.value.postalCode)
        reloaded.toggleFavorite(Favorite("k1", "x", null, "x", 2))
        assertTrue(UserDataRepository(JsonFileStore(dir)).favorites.value.isEmpty())
    }

    @Test fun `corrupt files are ignored`() {
        java.io.File(dir, "prefs.json").writeText("{kaputt")
        assertEquals(null, UserDataRepository(JsonFileStore(dir)).prefs.value.postalCode)
    }

    @Suppress("unused") private val unusedValidity = Validity()
}
