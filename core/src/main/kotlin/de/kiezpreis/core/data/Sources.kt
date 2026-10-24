package de.kiezpreis.core.data

import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.Retailer

enum class SourceKind(val label: String) {
    OPEN_DATA("Offene Daten"),
    RETAILER_WEB("Händler-Website"),
    SAMPLE("Beispieldaten"),
}

/** How far a source is from being trustworthy in production. Shown to the user as is. */
enum class SourceReadiness(val label: String) {
    /** Implemented, tested against recorded responses, legally usable. */
    AVAILABLE("verfügbar"),
    /** Implemented, but depends on an undocumented endpoint that may change at any time. */
    EXPERIMENTAL("experimentell"),
    /** Researched and planned, not yet implemented. */
    PLANNED("geplant"),
    /** No legal and technically stable way found. */
    UNAVAILABLE("nicht verfügbar"),
}

data class SourceDescriptor(
    val id: String,
    val name: String,
    val retailer: Retailer?,
    val kind: SourceKind,
    val readiness: SourceReadiness,
    /** Where the source's prices apply. */
    val coverage: String,
    val license: String,
    val refreshIntervalHours: Int,
    val homepage: String?,
    val notes: String,
)

data class FetchContext(
    val nowMillis: Long,
    /** Optional postal code chosen by the user. */
    val postalCode: String?,
)

enum class FailureReason(val label: String) {
    NETWORK("Quelle nicht erreichbar"),
    HTTP("Quelle antwortet mit Fehler"),
    PARSE("Antwort nicht lesbar"),
    BLOCKED("Zugriff vom Anbieter abgelehnt"),
    NOT_IMPLEMENTED("noch nicht angebunden"),
}

sealed interface FetchResult {
    data class Success(val offers: List<Offer>, val fetchedAt: Long, val rejected: Int = 0) : FetchResult
    data class Failure(val reason: FailureReason, val message: String, val retryable: Boolean) : FetchResult
}

/** Adapter for one data source. Implementations must never invent data on failure. */
interface OfferSource {
    val descriptor: SourceDescriptor
    suspend fun fetch(context: FetchContext): FetchResult
}

/** Minimal HTTP abstraction so adapters can be tested without network. */
interface HttpClient {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse
}

data class HttpResponse(val code: Int, val body: String)

class HttpException(message: String, cause: Throwable? = null) : Exception(message, cause)
