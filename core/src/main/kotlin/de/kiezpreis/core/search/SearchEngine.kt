package de.kiezpreis.core.search

import de.kiezpreis.core.logic.PriceMath
import de.kiezpreis.core.logic.ValidityRules
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PriceKind
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.region.RegionalRules
import java.time.LocalDate

enum class SortOrder(val label: String) {
    RELEVANCE("Relevanz"),
    PRICE("Preis"),
    UNIT_PRICE("Grundpreis"),
    DISCOUNT("Rabatt"),
    ENDING_SOON("Endet bald"),
}

data class SearchFilters(
    val retailers: Set<Retailer> = emptySet(),
    val categories: Set<Category> = emptySet(),
    val maxPrice: Cents? = null,
    val onlyConfirmedBerlin: Boolean = false,
    val onlyWithDiscount: Boolean = false,
    val includeObservations: Boolean = true,
) {
    val activeCount: Int
        get() = listOf(retailers.isNotEmpty(), categories.isNotEmpty(), maxPrice != null, onlyConfirmedBerlin, onlyWithDiscount, !includeObservations)
            .count { it }
}

/** How well an offer matches. Partial matches are only used as "similar" suggestions. */
enum class MatchQuality { EXACT, FUZZY }

data class SearchHit(val group: ProductGroup, val score: Double, val quality: MatchQuality)

data class SearchResult(
    val query: String,
    val hits: List<SearchHit>,
    /** Offers that matched before filters were applied (to tell "filtered away" from "nothing found"). */
    val unfilteredMatchCount: Int,
)

class SearchEngine(private val today: () -> LocalDate = { LocalDate.now() }) {

    /** Small domain thesaurus: query word -> words that count as the same thing. */
    private val synonyms: Map<String, List<String>> = mapOf(
        "proteinpulver" to listOf("protein", "whey", "eiweisspulver", "eiweiss"),
        "eiweisspulver" to listOf("protein", "whey", "proteinpulver"),
        "hahnchen" to listOf("hahnchen", "hendl", "chicken", "huhn"),
        "hahnchenbrust" to listOf("hahnchenbrustfilet", "hahnchenbrust", "hahnchenbrustfilets"),
        "cola" to listOf("cola", "cocacola"),
        "waschmittel" to listOf("waschmittel", "vollwaschmittel", "colorwaschmittel", "waschpulver", "waschgel", "flussigwaschmittel", "persil", "ariel", "tandil", "spee", "formil"),
        "klopapier" to listOf("toilettenpapier"),
        "toilettenpapier" to listOf("klopapier"),
        "hackfleisch" to listOf("hackfleisch", "gehacktes", "hack"),
    )

    fun search(
        query: String,
        offers: List<Offer>,
        filters: SearchFilters = SearchFilters(),
        sort: SortOrder = SortOrder.RELEVANCE,
    ): SearchResult {
        val queryTokens = TextNormalizer.tokens(query).filter { it.isNotBlank() }
        val day = today()
        val candidates = offers
            .filter(RegionalRules::isVisibleInBerlin)
            .filter { ValidityRules.isCurrent(it, day) }
            .map(RegionalRules::berlinStoresOnly)

        val scored = if (queryTokens.isEmpty()) {
            candidates.map { it to (1.0 to MatchQuality.EXACT) }
        } else {
            candidates.mapNotNull { offer -> score(queryTokens, offer)?.let { offer to it } }
        }
        val unfilteredCount = scored.size
        val filtered = scored.filter { (offer, _) -> passes(offer, filters) }

        val groups = Grouping.group(filtered.map { it.first })
        val scoreByOffer = filtered.associate { it.first.id to it.second }
        val hits = groups.map { g ->
            val best = g.offers.map { scoreByOffer.getValue(it.id) }.maxByOrNull { it.first }!!
            SearchHit(g, best.first, best.second)
        }
        return SearchResult(query, sortHits(hits, sort), unfilteredCount)
    }

    fun passes(offer: Offer, f: SearchFilters): Boolean {
        if (f.retailers.isNotEmpty() && offer.retailer !in f.retailers) return false
        if (f.categories.isNotEmpty() && offer.category !in f.categories) return false
        if (f.maxPrice != null && (offer.price == null || offer.price > f.maxPrice)) return false
        if (f.onlyConfirmedBerlin && !RegionalRules.isConfirmedForBerlin(offer)) return false
        if (f.onlyWithDiscount && PriceMath.discountPercent(offer) == null) return false
        if (!f.includeObservations && offer.kind == PriceKind.OBSERVATION) return false
        return true
    }

    private fun sortHits(hits: List<SearchHit>, sort: SortOrder): List<SearchHit> {
        val day = today()
        return when (sort) {
            SortOrder.RELEVANCE -> hits.sortedWith(
                compareByDescending<SearchHit> { it.quality == MatchQuality.EXACT }
                    .thenByDescending { it.score }
                    .thenBy { it.group.lowestPrice()?.value ?: Long.MAX_VALUE },
            )
            SortOrder.PRICE -> hits.sortedWith(nullsLastBy { it.group.lowestPrice()?.value })
            SortOrder.UNIT_PRICE -> hits.sortedWith(nullsLastBy { it.group.bestUnitPrice()?.cents?.value })
            SortOrder.DISCOUNT -> hits.sortedWith(nullsLastBy { h ->
                h.group.offers.mapNotNull { PriceMath.discountPercent(it) }.maxOrNull()?.let { -it.toLong() }
            })
            SortOrder.ENDING_SOON -> hits.sortedWith(nullsLastBy { h ->
                h.group.offers.mapNotNull { ValidityRules.parse(it.validity.to) }.minOrNull()
                    ?.takeIf { !it.isBefore(day) }?.toEpochDay()
            })
        }
    }

    private fun nullsLastBy(selector: (SearchHit) -> Long?): Comparator<SearchHit> =
        compareBy<SearchHit, Long?>(nullsLast()) { selector(it) }.thenByDescending { it.score }

    /** Every query word must match some word of the offer; returns null otherwise. */
    internal fun score(queryTokens: List<String>, offer: Offer): Pair<Double, MatchQuality>? {
        val textTokens = (
            TextNormalizer.tokens(offer.title) +
                offer.brand?.let { TextNormalizer.tokens(it) }.orEmpty() +
                TextNormalizer.tokens(offer.category.label) +
                TextNormalizer.tokens(offer.retailer.displayName)
            ).distinct()
        var total = 0.0
        var fuzzy = false
        var matched = 0
        for (q in queryTokens) {
            val alternatives = listOf(q) + synonyms[q].orEmpty()
            val best = alternatives.maxOf { alt -> textTokens.maxOfOrNull { tokenScore(alt, it) } ?: 0.0 }
            if (best <= 0.0) {
                if (ProductIdentity.isGeneric(q)) { total += 0.5; continue }
                return null
            }
            if (best < 0.8) fuzzy = true
            total += best
            matched++
        }
        if (matched == 0) return null
        // Prefer offers whose title is mostly covered by the query (shorter, more specific titles).
        val titleTokens = TextNormalizer.tokens(offer.title).size.coerceAtLeast(1)
        val coverage = queryTokens.size.toDouble() / titleTokens
        val score = total / queryTokens.size + 0.1 * coverage.coerceAtMost(1.0)
        return score to if (fuzzy) MatchQuality.FUZZY else MatchQuality.EXACT
    }

    private fun tokenScore(q: String, t: String): Double = when {
        q == t -> 1.0
        q.length >= 3 && t.startsWith(q) -> 0.9
        q.length >= 5 && t.contains(q) -> 0.85 // compound words: "brust" in "hahnchenbrustfilet"
        t.length >= 4 && q.startsWith(t) && q.length - t.length <= 2 -> 0.8 // plural/inflection: "butter" vs "butters"
        Fuzzy.isTypo(q, t) -> 0.7
        q.length >= 6 && t.length > q.length && Fuzzy.isTypo(q, t.take(q.length)) -> 0.6
        else -> 0.0
    }

    /** Autocomplete suggestions from product titles, brands and categories. */
    fun suggestions(prefix: String, offers: List<Offer>, limit: Int = 6): List<String> {
        val p = TextNormalizer.fold(prefix.trim())
        if (p.length < 2) return emptyList()
        val candidates = LinkedHashSet<String>()
        offers.forEach { o ->
            o.brand?.let(candidates::add)
            candidates.add(cleanTitle(o.title))
        }
        Category.entries.forEach { candidates.add(it.label) }
        return candidates
            .filter { c -> TextNormalizer.fold(c).let { it.startsWith(p) || it.contains(" $p") } }
            .sortedBy { it.length }
            .take(limit)
    }

    private fun cleanTitle(title: String) = title.substringBefore(",").trim()
}
