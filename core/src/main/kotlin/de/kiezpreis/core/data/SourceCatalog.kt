package de.kiezpreis.core.data

import de.kiezpreis.core.model.Retailer

/**
 * Every source that was evaluated, including the ones that cannot be used.
 * The settings screen lists this catalog so the user sees honestly what is and is not integrated.
 * Details and evidence: DATA_SOURCES.md.
 */
object SourceCatalog {

    val sample = SourceDescriptor(
        id = "sample",
        name = "Beispieldaten",
        retailer = null,
        kind = SourceKind.SAMPLE,
        readiness = SourceReadiness.AVAILABLE,
        berlinCoverage = "Keine. Erfundene Demonstrationswerte, keine echten Preise.",
        license = "–",
        refreshIntervalHours = 24,
        homepage = null,
        notes = "Nur zum Ausprobieren der Oberfläche. Jede Karte ist als Beispiel markiert.",
    )

    val openPrices = SourceDescriptor(
        id = "open_prices",
        name = "Open Prices (Open Food Facts)",
        retailer = null,
        kind = SourceKind.OPEN_DATA,
        readiness = SourceReadiness.AVAILABLE,
        berlinCoverage = "Einzelne, von Freiwilligen erfasste Preise aus konkreten Berliner Filialen (mit PLZ). Lückenhaft.",
        license = "ODbL 1.0, Namensnennung erforderlich",
        refreshIntervalHours = 12,
        homepage = "https://prices.openfoodfacts.org",
        notes = "Offizielle, dokumentierte API. Liefert Preisbeobachtungen, keine Prospektangebote.",
    )

    val webImport = SourceDescriptor(
        id = "web_import",
        name = "Von Händlerseiten übernommen",
        retailer = null,
        kind = SourceKind.RETAILER_WEB,
        readiness = SourceReadiness.EXPERIMENTAL,
        berlinCoverage = "Filialgenau, wenn du beim Übernehmen deinen Berliner Markt angibst. Sonst „Berlin nicht bestätigt“.",
        license = "Nutzungsbedingungen des jeweiligen Händlers; nur für deinen privaten Gebrauch auf diesem Gerät",
        refreshIntervalHours = 1,
        homepage = null,
        notes = "Du öffnest die Angebotsseite des Händlers in der App und tippst auf „Angebote übernehmen“. " +
            "Die App liest nur, was die Seite dir gerade zeigt, und prüft es streng. Es läuft nichts im Hintergrund, " +
            "Sicherheitsabfragen des Händlers löst du selbst. Ohne Enddatum verschwinden Preise nach 7 Tagen.",
    )

    val lidlLeaflet = SourceDescriptor(
        id = "lidl_leaflet",
        name = "Lidl Online-Prospekt",
        retailer = Retailer.LIDL,
        kind = SourceKind.RETAILER_WEB,
        readiness = SourceReadiness.PLANNED,
        berlinCoverage = "Bundesweiter Prospekt. Strukturiert nur Non-Food und Getränke, Lebensmittel nur als Bild.",
        license = "Nutzungsbedingungen Lidl, kein offizielles API",
        refreshIntervalHours = 24,
        homepage = Retailer.LIDL.website,
        notes = "Undokumentierter Prospekt-Endpunkt, ohne Schutzmaßnahmen erreichbar. Für Phase 3 vorgesehen.",
    )

    val aldiNord = SourceDescriptor(
        id = "aldi_nord_web",
        name = "ALDI Nord Angebotsseite",
        retailer = Retailer.ALDI_NORD,
        kind = SourceKind.RETAILER_WEB,
        readiness = SourceReadiness.PLANNED,
        berlinCoverage = "Berlin gehört zu ALDI Nord. Angebote überwiegend bundesweit für ALDI Nord.",
        license = "Nutzungsbedingungen ALDI Nord, kein offizielles API",
        refreshIntervalHours = 24,
        homepage = Retailer.ALDI_NORD.website,
        notes = "Daten stecken als JSON in der Angebotsseite. Muss auf dem Gerät geprüft werden.",
    )

    private const val VIA_IMPORT = " Über „Von Händlerseiten übernehmen“ kannst du die Angebote selbst in die App holen."

    val unavailable = listOf(
        SourceDescriptor(
            "rewe_web", "REWE Marktangebote", Retailer.REWE, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau (z. B. REWE Heerstr. 560, Berlin), aber nur im Browser gerendert.",
            "Nutzungsbedingungen REWE", 24, Retailer.REWE.website,
            "Kein automatischer Abruf: kein offenes API, App-API nur mit Client-Zertifikat aus der REWE-App." + VIA_IMPORT,
        ),
        SourceDescriptor(
            "edeka_web", "EDEKA Marktangebote", Retailer.EDEKA, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau (Berlin = EDEKA Minden-Hannover).",
            "Nutzungsbedingungen EDEKA", 24, Retailer.EDEKA.website,
            "Kein automatischer Abruf: der JSON-Dienst lehnt Zugriffe außerhalb des Browsers ab." + VIA_IMPORT,
        ),
        SourceDescriptor(
            "penny_web", "PENNY Angebote", Retailer.PENNY, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Regional nach Verkaufsregion des Marktes.",
            "Nutzungsbedingungen PENNY", 24, Retailer.PENNY.website,
            "Kein automatischer Abruf: der REST-Endpunkt ist nur aus der eigenen Website heraus nutzbar." + VIA_IMPORT,
        ),
        SourceDescriptor(
            "kaufland_web", "Kaufland Filialangebote", Retailer.KAUFLAND, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau.", "Nutzungsbedingungen Kaufland", 24, Retailer.KAUFLAND.website,
            "Kein automatischer Abruf: Bot-Challenge vor den Angeboten, die wir nicht umgehen." + VIA_IMPORT,
        ),
        SourceDescriptor(
            "netto_web", "Netto Filialangebote", Retailer.NETTO_MD, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau.", "Nutzungsbedingungen Netto", 24, Retailer.NETTO_MD.website,
            "Kein automatischer Abruf: robots.txt untersagt das Abrufen der Datenendpunkte." + VIA_IMPORT,
        ),
    )

    val all: List<SourceDescriptor> = listOf(openPrices, webImport, sample, lidlLeaflet, aldiNord) + unavailable
}
