package de.kiezpreis.core.data

import de.kiezpreis.core.model.Offer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** Status of one source as shown in the UI. */
sealed interface SourceStatus {
    data object NeverLoaded : SourceStatus
    data object Loading : SourceStatus
    data class Ok(val at: Long, val count: Int, val rejected: Int) : SourceStatus
    data class Failed(val at: Long, val reason: FailureReason, val message: String) : SourceStatus
    data object Disabled : SourceStatus
}

data class SourceState(
    val descriptor: SourceDescriptor,
    val enabled: Boolean,
    val status: SourceStatus,
    /** When the offers currently shown from this source were fetched (may be older than [status]). */
    val dataFetchedAt: Long?,
    val isStale: Boolean,
)

data class DataState(
    val offers: List<Offer> = emptyList(),
    val sources: List<SourceState> = emptyList(),
    val isRefreshing: Boolean = false,
) {
    val hasAnyData: Boolean get() = offers.isNotEmpty()
    val onlySampleData: Boolean get() = offers.isNotEmpty() && offers.all { it.isSample }
    val failedSources: List<SourceState> get() = sources.filter { it.enabled && it.status is SourceStatus.Failed }
    val enabledRealSources: List<SourceState> get() = sources.filter { it.enabled && it.descriptor.kind != SourceKind.SAMPLE }
    val lastSuccessfulUpdate: Long? get() = sources.filter { it.enabled }.mapNotNull { it.dataFetchedAt }.maxOrNull()
}

@Serializable
private data class CachedOffers(val fetchedAt: Long, val offers: List<Offer>)

/**
 * Combines all enabled sources. Each source fails independently: a failing source keeps its last
 * good data (marked stale) and never gets replacement values.
 */
class OfferRepository(
    private val sources: List<OfferSource>,
    private val store: JsonFileStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxAttempts: Int = 3,
    private val retryDelayMillis: Long = 1_000,
    private val log: (String) -> Unit = {},
) {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, CachedOffers>()
    private val statuses = mutableMapOf<String, SourceStatus>()
    private var enabledIds: Set<String> = emptySet()

    private val _state = MutableStateFlow(DataState())
    val state: StateFlow<DataState> = _state.asStateFlow()

    init {
        sources.forEach { s -> store.read(cacheKey(s.descriptor.id), CachedOffers.serializer())?.let { cache[s.descriptor.id] = it } }
    }

    fun setEnabledSources(ids: Set<String>) {
        enabledIds = ids
        publish()
    }

    /** Refreshes enabled sources whose data is older than their interval, or all when [force]. */
    suspend fun refresh(force: Boolean = false, context: FetchContext = FetchContext(clock(), null), only: Set<String>? = null) {
        mutex.withLock {
            val due = sources.filter { it.descriptor.id in enabledIds && (only == null || it.descriptor.id in only) }.filter { s ->
                force || cache[s.descriptor.id]?.let { clock() - it.fetchedAt > s.descriptor.refreshIntervalHours * HOUR } ?: true
            }
            if (due.isEmpty()) {
                publish()
                return
            }
            due.forEach { statuses[it.descriptor.id] = SourceStatus.Loading }
            _state.update { it.copy(isRefreshing = true) }
            publish(refreshing = true)
            coroutineScope {
                due.map { source -> async { source to fetchWithRetry(source, context) } }.awaitAll()
            }.forEach { (source, result) -> apply(source, result) }
            publish(refreshing = false)
        }
    }

    /** Runs a single source once without touching the cache: used by "Quelle testen". */
    suspend fun test(sourceId: String): FetchResult {
        val source = sources.firstOrNull { it.descriptor.id == sourceId }
            ?: return FetchResult.Failure(FailureReason.NOT_IMPLEMENTED, "Kein Adapter vorhanden", false)
        return runCatching { source.fetch(FetchContext(clock(), null)) }
            .getOrElse { FetchResult.Failure(FailureReason.NETWORK, it.message ?: "Fehler", true) }
    }

    private suspend fun fetchWithRetry(source: OfferSource, context: FetchContext): FetchResult {
        var last: FetchResult.Failure? = null
        repeat(maxAttempts) { attempt ->
            val result = runCatching { source.fetch(context) }
                .getOrElse { FetchResult.Failure(FailureReason.NETWORK, it.message ?: it.javaClass.simpleName, true) }
            when (result) {
                is FetchResult.Success -> return result
                is FetchResult.Failure -> {
                    last = result
                    log("source=${source.descriptor.id} attempt=${attempt + 1} reason=${result.reason}")
                    if (!result.retryable) return result
                    if (attempt < maxAttempts - 1) delay(retryDelayMillis * (1L shl attempt))
                }
            }
        }
        return last!!
    }

    private fun apply(source: OfferSource, result: FetchResult) {
        val id = source.descriptor.id
        when (result) {
            is FetchResult.Success -> {
                val valid = result.offers.mapNotNull(OfferValidator::sanitize)
                val rejected = result.rejected + (result.offers.size - valid.size)
                val entry = CachedOffers(result.fetchedAt, valid)
                cache[id] = entry
                runCatching { store.write(cacheKey(id), CachedOffers.serializer(), entry) }
                statuses[id] = SourceStatus.Ok(result.fetchedAt, valid.size, rejected)
            }
            is FetchResult.Failure -> statuses[id] = SourceStatus.Failed(clock(), result.reason, result.message)
        }
    }

    private fun publish(refreshing: Boolean = _state.value.isRefreshing) {
        val now = clock()
        val sourceStates = sources.map { s ->
            val id = s.descriptor.id
            val enabled = id in enabledIds
            val cached = cache[id]
            SourceState(
                descriptor = s.descriptor,
                enabled = enabled,
                status = if (!enabled) SourceStatus.Disabled else statuses[id] ?: SourceStatus.NeverLoaded,
                dataFetchedAt = cached?.fetchedAt,
                isStale = cached != null && now - cached.fetchedAt > 2 * s.descriptor.refreshIntervalHours * HOUR,
            )
        }
        val offers = sources.filter { it.descriptor.id in enabledIds }
            .flatMap { cache[it.descriptor.id]?.offers.orEmpty() }
        _state.value = DataState(offers = offers, sources = sourceStates, isRefreshing = refreshing)
    }

    private fun cacheKey(id: String) = "cache_$id"

    private companion object {
        const val HOUR = 3_600_000L
    }
}

/** Last line of defence against broken source data. */
object OfferValidator {
    /** Returns the offer with implausible parts removed, or null if it cannot be shown at all. */
    fun sanitize(offer: Offer): Offer? {
        if (offer.title.isBlank()) return null
        val price = offer.price
        if (price != null && (price.value <= 0 || price.value > 100_000)) return null
        val from = offer.validity.from
        val to = offer.validity.to
        if (from != null && to != null && to < from) return null
        val ref = offer.referencePrice
        // A "former price" that is not higher than the current price is no evidence of a discount.
        if (ref != null && (price == null || ref.price <= price)) return offer.copy(referencePrice = null)
        return offer
    }
}
