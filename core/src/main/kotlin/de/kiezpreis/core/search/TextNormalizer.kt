package de.kiezpreis.core.search

/**
 * Normalises product text for matching: case, umlauts (ä = ae = a), ß, hyphenated brand names
 * ("Coca-Cola" matches "coca cola" and "cocacola") and punctuation.
 */
object TextNormalizer {

    private val nonAlnum = Regex("[^a-z0-9]+")

    fun fold(text: String): String {
        val lower = text.lowercase()
            .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
            .replace("é", "e").replace("è", "e").replace("à", "a").replace("ç", "c")
        // Applied to query and product text alike, so "Haehnchen" and "Hähnchen" meet in "hahnchen".
        return lower.replace("ae", "a").replace("oe", "o").replace("ue", "u")
    }

    /** Tokens of a text. Hyphenated words yield their parts and the joined form. */
    fun tokens(text: String): List<String> {
        val out = LinkedHashSet<String>()
        for (raw in text.split(Regex("\\s+"))) {
            val folded = fold(raw)
            if (folded.contains('-') || folded.contains('/') || folded.contains('.')) {
                val parts = folded.split(nonAlnum).filter { it.isNotEmpty() }
                out.addAll(parts)
                if (parts.size > 1) out.add(parts.joinToString(""))
            } else {
                folded.split(nonAlnum).filter { it.isNotEmpty() }.forEach(out::add)
            }
        }
        return out.toList()
    }

    /** Normalised single-line form, used as a cache key for recent searches. */
    fun normalizeQuery(text: String): String = tokens(text).joinToString(" ")
}
