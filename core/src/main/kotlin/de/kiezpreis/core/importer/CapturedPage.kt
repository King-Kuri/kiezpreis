package de.kiezpreis.core.importer

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the in-app browser hands over when the user taps "Angebote übernehmen": the structured data
 * the retailer page itself contains or received while the user was browsing it. Nothing in here is
 * fetched by the app on its own.
 */
@Serializable
data class CapturedPage(
    val url: String,
    val title: String? = null,
    /** Contents of `<script type="application/ld+json">` blocks (schema.org). */
    val jsonLd: List<String> = emptyList(),
    /** Contents of `<script type="application/json">` blocks, e.g. `__NEXT_DATA__`. */
    val embeddedJson: List<String> = emptyList(),
    /** JSON responses the page loaded itself (its own fetch/XHR calls). */
    val responses: List<CapturedResponse> = emptyList(),
    /** Product tiles read from the visible page where a price and a title sit in the same tile. */
    val domCards: List<DomCard> = emptyList(),
) {
    val host: String get() = runCatching { java.net.URI(url).host.orEmpty().lowercase() }.getOrDefault("")

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun decode(text: String): CapturedPage = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class CapturedResponse(val url: String, val body: String)

/** Text pieces of one product tile, exactly as shown on the page. */
@Serializable
data class DomCard(
    val title: String,
    val price: String,
    val oldPrice: String? = null,
    val basePrice: String? = null,
    val size: String? = null,
    val brand: String? = null,
    val note: String? = null,
    val image: String? = null,
    /** Chain name or logo text found in the tile, on portals. */
    val retailer: String? = null,
)
