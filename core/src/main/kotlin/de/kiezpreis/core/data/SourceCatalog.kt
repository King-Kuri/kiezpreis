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

    val unavailable = listOf(
        SourceDescriptor(
            "rewe_web", "REWE Marktangebote", Retailer.REWE, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau (z. B. REWE Heerstr. 560, Berlin), aber nur im Browser gerendert.",
            "Nutzungsbedingungen REWE", 24, Retailer.REWE.website,
            "Kein offenes API. App-API nur mit Client-Zertifikat aus der REWE-App, das wir nicht verwenden.",
        ),
        SourceDescriptor(
            "edeka_web", "EDEKA Marktangebote", Retailer.EDEKA, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau (Berlin = EDEKA Minden-Hannover).",
            "Nutzungsbedingungen EDEKA", 24, Retailer.EDEKA.website,
            "JSON-Dienst existiert, lehnt aber Zugriffe außerhalb des Browsers per Bot-Schutz ab.",
        ),
        SourceDescriptor(
            "penny_web", "PENNY Angebote", Retailer.PENNY, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Regional nach Verkaufsregion des Marktes.",
            "Nutzungsbedingungen PENNY", 24, Retailer.PENNY.website,
            "REST-Endpunkt nur aus der eigenen Website heraus nutzbar (Bot-Schutz).",
        ),
        SourceDescriptor(
            "kaufland_web", "Kaufland Filialangebote", Retailer.KAUFLAND, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau.", "Nutzungsbedingungen Kaufland", 24, Retailer.KAUFLAND.website,
            "Bot-Challenge vor den Angeboten. Wird nicht umgangen.",
        ),
        SourceDescriptor(
            "netto_web", "Netto Filialangebote", Retailer.NETTO_MD, SourceKind.RETAILER_WEB, SourceReadiness.UNAVAILABLE,
            "Filialgenau.", "Nutzungsbedingungen Netto", 24, Retailer.NETTO_MD.website,
            "robots.txt untersagt das automatisierte Abrufen der Datenendpunkte.",
        ),
    )

    val all: List<SourceDescriptor> = listOf(openPrices, sample, lidlLeaflet, aldiNord) + unavailable
}
