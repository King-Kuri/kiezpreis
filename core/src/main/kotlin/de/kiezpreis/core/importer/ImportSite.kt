package de.kiezpreis.core.importer

import de.kiezpreis.core.model.Retailer

/**
 * A website the user can open in the app and take prices from: a retailer's own site, or an offer
 * portal that lists several retailers. On a portal the retailer is read per offer.
 */
data class ImportSite(
    val id: String,
    val name: String,
    val startUrl: String,
    /** Registrable domain; navigation stays on it and its subdomains. */
    val domain: String,
    /** The retailer for a retailer site; null for a portal. */
    val retailer: Retailer?,
    /** Shown on the sources screen, e.g. the portal's own usage terms. */
    val note: String? = null,
) {
    val isPortal: Boolean get() = retailer == null

    fun allows(host: String): Boolean {
        val h = host.lowercase().trimEnd('.')
        return h == domain || h.endsWith(".$domain")
    }
}

object ImportSites {

    private val retailerDomains = mapOf(
        Retailer.ALDI_NORD to "aldi-nord.de",
        Retailer.ALDI_SUED to "aldi-sued.de",
        Retailer.LIDL to "lidl.de",
        Retailer.REWE to "rewe.de",
        Retailer.EDEKA to "edeka.de",
        Retailer.KAUFLAND to "kaufland.de",
        Retailer.PENNY to "penny.de",
        Retailer.NETTO_MD to "netto-online.de",
    )

    val retailerSites: List<ImportSite> = retailerDomains.map { (r, domain) -> ImportSite(r.name, r.displayName, r.website, domain, r) }

    val portals: List<ImportSite> = listOf(
        ImportSite(
            id = "aktionspreis",
            name = "aktionspreis.de",
            startUrl = "https://www.aktionspreis.de",
            domain = "aktionspreis.de",
            retailer = null,
            note = "Angebotsportal mit vielen Ketten. Laut AGB nur für private Zwecke; Kiezpreis speichert nur auf deinem Gerät.",
        ),
    )

    val all: List<ImportSite> = retailerSites + portals

    fun forHost(host: String): ImportSite? = all.firstOrNull { it.allows(host) }

    fun byId(id: String): ImportSite? = all.firstOrNull { it.id == id }
}

/** Finds the chain an offer on a portal belongs to. Ambiguous names give null, never a guess. */
object RetailerNames {
    private val patterns: List<Pair<Retailer, Regex>> = listOf(
        Retailer.ALDI_SUED to Regex("""\baldi[\s-]*(süd|sued|sud)\b"""),
        Retailer.ALDI_NORD to Regex("""\baldi[\s-]*nord\b"""),
        Retailer.LIDL to Regex("""\blidl\b"""),
        Retailer.REWE to Regex("""\brewe\b"""),
        Retailer.EDEKA to Regex("""\bedeka\b"""),
        Retailer.KAUFLAND to Regex("""\bkaufland\b"""),
        Retailer.PENNY to Regex("""\bpenny\b"""),
        Retailer.NETTO_MD to Regex("""\bnetto[\s-]*(marken[\s-]*discount|md)\b"""),
    )

    /** The single chain named in [text], or null when none or several are named. */
    fun find(text: String?): Retailer? {
        if (text.isNullOrBlank()) return null
        val lower = text.lowercase()
        val hits = patterns.filter { (_, re) -> re.containsMatchIn(lower) }.map { it.first }.toSet()
        return hits.singleOrNull()
    }
}
