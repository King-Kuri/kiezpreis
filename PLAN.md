# Plan und Status

Stand: 24.10.2026. ✅ fertig und getestet · 🟡 implementiert, Prüfung offen · ⬜ offen

## Phase 1 – Analyse und Planung ✅
* ✅ Umgebung geprüft: JDK 21, Gradle 8.14.3; kein Android SDK, Google-Maven gesperrt → D-03
* ✅ Datenquellen recherchiert und bewertet → DATA_SOURCES.md
* ✅ README, PLAN, DATA_SOURCES, ARCHITECTURE, DECISIONS

## Phase 2 – UI und Prototyp ✅ (APK-Build in CI 🟡)
* ✅ Designsystem: Farben Dark/Light, Typografie, Glasflächen, Aurora-Hintergrund, Motion
* ✅ Navigation: schwebende Glas-Navigation, Detail- und Quellen-Ebenen, Back-Geste
* ✅ Startseite, Suche mit Filter-Sheet, Ergebnisliste, Produktdetail, Merkliste, Einstellungen, Datenquellen
* ✅ Beispieldaten, durchgehend gekennzeichnet
* ✅ Zustände: lädt, keine Daten, Quelle nicht erreichbar, kein Treffer, weggefiltert, kein Preis
* ✅ 29 Screenshots (14 Ansichten in Dark und Light, plus Leerzustand) automatisch gerendert
* 🟡 Debug-APK über GitHub Actions; Start auf echtem Gerät noch nicht beobachtet

## Phase 3 – Echte Daten
* 🟡 Open Prices: Adapter, Validierung, Berlin-Filter, Cache, Fehlerzustände mit Tests; Live-Abruf auf Gerät prüfen
* ⬜ Lidl-Prospekt-Adapter (experimentell, nur strukturierte Artikel)
* ⬜ ALDI-Nord-Adapter (experimentell)
* ✅ Entscheidung Noel (24.10.2026): WebView-Weg ja → D-17
* 🟡 Übernahme von Händlerseiten (alle 7 Händler): sichtbare WebView, Erfassung auf Knopfdruck, strenge
  Erkennung, Vorschau mit Haken, Marktangabe per Berliner PLZ; Kern mit Tests, auf echten Händlerseiten noch nicht erprobt
* ⬜ Produktbilder laden, wenn eine Quelle sie liefert (Coil)

## Phase 4 – Erweiterte Funktionen
* ✅ Filter, Sortierung, Merkliste, PLZ (nur Berlin), Händlerpräferenzen, Offline-Cache
* ⬜ Preisalarme mit WorkManager (nur mit regelmäßig prüfbarer Quelle, siehe ARCHITECTURE.md)
* ⬜ PLZ für filialgenaue Quellen nutzen (Entfernung/Bezirk)

## Phase 5 – QS und Build
* ✅ Unit-Tests Kern (siehe unten)
* 🟡 Android-Build in CI inkl. Secret-Scan der APK
* ⬜ Compose-UI-Tests auf Android (Robolectric oder Emulator in CI)
* ⬜ Manuelle Prüfung auf Gerät: TalkBack, große Schrift, kleine Displays

## Testprotokoll

`./gradlew :core:test`, 57 Tests, alle grün (24.10.2026):

| Testklasse | Prüft |
|---|---|
| PackSizeParserTest | REWE-Texte mit Grundpreis in Klammern, Kästen „20 x 0,5 l“, ×, g/kg/cl/Stück, Bereiche und Mehrdeutiges → unbekannt |
| PriceMathTest | Grundpreis €/l, €/kg, €/Stück, Rundung, Mehrfachpackung; Rabatt nur mit höherem Vergleichspreis, abgerundet |
| RegionTest | Berliner PLZ, Brandenburg (Potsdam, Schönefeld, Dahlewitz), Stadtname vs. PLZ, gemischte Filialen |
| ValidityTest | aktiv/abgelaufen/kommend/unbekannt, Labels, Beobachtungen nach 21 Tagen abgelaufen |
| SearchEngineTest | Varianten getrennt, Potsdam ausgeschlossen, kommende Angebote, Grundpreis-Sieger, Umlaute, Tippfehler, Synonyme, Filter, „weggefiltert“ vs. „nichts gefunden“, Sortierung, fehlender Preis |
| OpenPricesSourceTest | Mapping, Brandenburg/CHF/Duplikat/leerer Name/„Netto“ verworfen, Preis pro kg, Netzwerk-, HTTP-, 429- und Parse-Fehler, Abfrage-URL |
| WebImportTest | Preistexte („1,99 €“, „-,99“, „2,-“; Datum, nackte Zahl, zwei Preise → verworfen), schema.org-Produkt, verschachtelte Seitendaten mit Cent-Feldern, Streich- und 30-Tage-Preis, Pfand/App-Bedingungen, mehrdeutige Ganzzahlen/CHF/zu teuer verworfen, Kacheln mit zwei Preisen verworfen, Händler-Domains exakt (kein ALDI Süd, kein `rewe.de.evil.example`), Berliner Markt → filialgenau, Potsdam abgelehnt, ohne Markt → nicht bestätigt, Preise ohne Datum nach 7 Tagen weg, Neuimport ersetzt alten |
| RepositoryTest | Quelle fällt aus → Cache bleibt, keine Ersatzwerte, 3 Versuche; nicht wiederholbare Fehler; Neustart lädt Cache; Merkliste/Verlauf/PLZ überleben Neustart; kaputte Dateien |

Nicht automatisiert geprüft: Live-Abruf von Open Prices aus der App (Container hat keinen Zugang;
Antwortformat per Web-Abruf am 24.10.2026 verifiziert), Verhalten auf echten Geräten, und die Übernahme
von echten Händlerseiten: Die Arbeitsumgebung erreicht keine Händler-Website, die Erkennung ist daher nur
gegen nachgebaute Seitendaten getestet. Screenshots 14 und 15 zeigen die Vorschau mit solchen Testdaten.
