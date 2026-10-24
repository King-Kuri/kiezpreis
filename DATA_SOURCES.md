# Datenquellen für Berliner Supermarktpreise

Recherche vom 24.10.2026. Bewertet nach: Berlin-Bezug, Erkennbarkeit regionaler Einschränkungen,
Qualität von Name/Preis/Packungsgröße, Angebotszeitraum, Rechtmäßigkeit und Dauerhaftigkeit, Kosten.

**Kurzfassung:** Kein Händler bietet eine offizielle, offene Schnittstelle für Angebote. Die einzige
offizielle, dokumentierte und frei lizenzierte Quelle mit echten Berliner Filialpreisen ist
**Open Prices**, allerdings mit sehr geringer Abdeckung. Die Händler-Webseiten liefern die eigentlich
gewünschten Prospektangebote, sind aber teils durch Bot-Schutz gesperrt oder per robots.txt untersagt.

## Bewertung

| Quelle | Berlin-Bezug | Regionale Einschränkung erkennbar | Daten­qualität | Zeitraum | Rechtlich / dauerhaft | Kosten | Entscheidung |
|---|---|---|---|---|---|---|---|
| **Open Prices** (prices.openfoodfacts.org) | ja, konkrete Filiale mit PLZ | ja (OSM-Standort) | Preis, oft EAN und Menge; Name manchmal leer | Beobachtungsdatum, kein Angebotszeitraum | offizielle API, ODbL 1.0 | kostenlos | **angebunden** |
| REWE Marktseiten (rewe.de/angebote/berlin-…/<markt>) | ja, filialgenau | ja | Name, Preis, Grundpreis, Pfand | nur Wochenzeitraum | kein API; Daten werden per Skript nachgeladen; App-API nur mit Client-Zertifikat aus der App | kostenlos | nicht angebunden |
| EDEKA (`edeka.de/eh/service/eh/offers?marketId=…`) | ja, filialgenau (Berlin = EDEKA Minden-Hannover) | ja | gut, Preis 0 = ohne Preis | ja | JSON-Dienst lehnt Zugriffe außerhalb des Browsers ab (Bot-Schutz) | kostenlos | nicht angebunden |
| PENNY (`penny.de/.rest/offers/by-category/<woche>/<kategorie>`) | regional (Verkaufsregion) | teilweise | sehr gut inkl. 30-Tage-Tiefstpreis, App-Preis | Woche | nur aus der Website heraus nutzbar (Bot-Schutz) | kostenlos | nicht angebunden |
| Lidl Online-Prospekt (`endpoints.leaflets.schwarz/v4/flyer`) | bundesweit | nein | strukturiert nur Non-Food und Getränke | ja | undokumentiert, ohne Schutz erreichbar | kostenlos | **geplant (Phase 3, experimentell)** |
| ALDI Nord Angebotsseite | Berlin liegt im ALDI-Nord-Gebiet; Angebote meist bundesweit Nord | selten | gut | ja | JSON in der HTML-Seite eingebettet, undokumentiert | kostenlos | **geplant (Phase 3, experimentell)** |
| Kaufland Filialangebote | filialgenau | ja | gut | ja | Bot-Challenge vor den Daten | kostenlos | nicht angebunden |
| Netto Marken-Discount | filialgenau | ja | gut | ja | robots.txt verbietet `/INTERSHOP/` (Datenendpunkte) und `/media/handzettel/` (am 24.10.2026 geprüft) | kostenlos | nicht angebunden |
| marktguru / kaufDA / meinprospekt (Aggregatoren) | per PLZ | teilweise | gut | ja | API-Schlüssel nur aus App/Website ausgelesen, keine Nutzungserlaubnis | kostenlos (inoffiziell) | abgelehnt |
| Apify-Actors u. ä. Scraping-Dienste | je nach Actor | je nach Actor | gut | ja | umgehen Schutz per Headless-Browser; Weiterverwendung ungeklärt | ca. 5–50 $/Monat | abgelehnt (kostenpflichtig, siehe Regel 5) |

## Belege

* Open Prices, Live-Abfrage am 24.10.2026 (`lat=52.52, lon=13.405, radius_km=25, date__gte=2026-09-01`):
  **45 Preise in knapp 8 Wochen**, davon **1 als rabattiert** markiert. Beispiele: REWE 12209 Berlin,
  EDEKA Neue Kantstraße 14057, Kaufland 10365, Lidl 13509, dm 10178. Ein Treffer stammte aus
  **Dahlewitz (15827, Brandenburg)**; genau dieser Fall ist als Test hinterlegt und wird verworfen.
  Dokumentation: <https://openfoodfacts.github.io/documentation/docs/Open-prices/prices/prices_list/>.
  Bedingungen: eigener User-Agent, ODbL mit Namensnennung.
* REWE-Marktseite Heerstr. 560, 13591 Berlin (abgerufen 24.10.2026): „Diese Woche 19.10. bis 25.10.“,
  12 „Top-Angebote in deinem Markt“ mit Preis und Grundpreis (z. B. Red Bull 0,25 l 0,99 €, 1 l = 3,96 €);
  Fußnote „Nur in teilnehmenden Märkten erhältlich“. Restliche Kategorien werden per Skript geladen.
* Erfahrungen eines offenen Scraping-Projekts (github.com/g6jjjz4gyb-sketch/prospekt, gelesen am 24.10.2026):
  „Chain APIs reject outside HTTP clients (403)“, alle sechs Ketten „sit behind Akamai“; EDEKA und
  Netto liefern selbst Playwright-Chromium „Access Denied“. Lidl-Lebensmittelangebote sind nur als
  Bild verfügbar. Diese Aussagen sind übernommen, nicht selbst im Detail nachgeprüft: Der Build-Container
  dieses Projekts erreicht die Händlerseiten nicht direkt.
* Apify „German Supermarket Offers“: Kaufland „serves a bot challenge“, Netto-Datenendpunkte per robots.txt
  gesperrt (für Netto selbst bestätigt, siehe oben).

## Was das für die App bedeutet

1. **Keine erfundene Vollständigkeit.** Die App zeigt nur, was eine Quelle belegt. Händler ohne Quelle
   erscheinen mit „keine Daten“.
2. **Open Prices zuerst**, weil legal, dokumentiert und filialgenau. Die Einträge sind **Preisbeobachtungen**
   (Regal- oder Kassenbon-Preise), keine Prospektangebote, und werden so gekennzeichnet. Älter als 21 Tage
   gilt als nicht mehr aktuell.
3. **Lidl-Prospekt und ALDI Nord** sind die nächsten Kandidaten, weil sie ohne Umgehung erreichbar sind.
   Beide sind bundesweit beworben; Berliner Gültigkeit wird nur als „bestätigt“ markiert, wenn der
   Prospekt das ausdrücklich sagt, sonst „nicht bestätigt“. Sie werden als **experimentell** geführt.
4. **Übernahme von Händlerseiten (seit 24.10.2026, experimentell, alle sieben Händler).** Noel hat den
   WebView-Weg freigegeben. Umgesetzt ist er so, dass die Person die Seite selbst bedient und die App
   nur auf Knopfdruck liest, was die Seite zeigt (D-17, D-18). Automatisch im Hintergrund wird nichts abgerufen.
5. **Netto** wird wegen robots.txt nicht automatisiert abgefragt. Die von der Person bediente Seite fällt
   nicht darunter, weil robots.txt sich an Crawler richtet.
6. **Nutzungsbedingungen:** Die REWE-App-Bedingungen (Stand 13.06.2023) erlauben nur private Nutzung und
   verbieten Kopieren und Extrahieren darüber hinaus. Sie gelten für die REWE-App, nicht ausdrücklich für die
   Website. Kiezpreis speichert übernommene Preise nur auf dem Gerät der Person und gibt nichts weiter.
   Eine Veröffentlichung im Play Store mit dieser Funktion sollte vorher rechtlich geprüft werden.
7. **Nicht live geprüft:** Diese Arbeitsumgebung darf keine Händler-Website abrufen. Die Erkennung beruht
   auf üblichen Seitenformaten (schema.org, Next.js-Daten, JSON-Antworten, Produktkacheln) und muss auf
   dem Gerät gegen die echten Seiten erprobt werden.

## Region

Berlin wird über Postleitzahlen bestimmt: 10115–10999, 12043–12689, 13051–13629, 14050–14199,
ohne bekannte Brandenburger Codes in diesen Bereichen (aktuell 12529 Schönefeld). Ein Preis mit
Brandenburger PLZ (z. B. 14467 Potsdam, 15827 Dahlewitz) wird nie als Berliner Preis gezeigt.

## Aktualität und Abrufintervalle

| Quelle | Intervall | Verhalten bei Fehler |
|---|---|---|
| Open Prices | 12 h, oder manuell | 3 Versuche mit wachsendem Abstand; danach letzter Stand mit Zeitstempel und „veraltet“ ab 24 h |
| Beispieldaten | lokal | – |
