# Architektur

## Überblick

```
┌──────────────────────────── app (Android) ────────────────────────────┐
│ MainActivity · AppViewModel · AndroidPlatform (Haze-Blur, Insets, Back)│
└───────────────┬────────────────────────────────────────────────────────┘
                │ kompiliert shared-ui/src mit
┌───────────────▼──────────── shared-ui (Compose) ───────────────────────┐
│ KiezApp (Navigation, Ebenen) · AppController (UI-Zustand)              │
│ screens/: Home, Search, FilterSheet, Detail, Favorites, Settings       │
│ components/: Glass, Aurora, NavBar, SearchField, Cards, Pills, States  │
│ theme/: Farben Dark/Light, Typografie, Motion, Glas-Einstellungen      │
└───────────────┬────────────────────────────────────────────────────────┘
┌───────────────▼──────────── core (reines Kotlin) ──────────────────────┐
│ model/   Retailer, Offer, PackSize, Cents, BerlinAvailability, Store …  │
│ logic/   PackSizeParser, PriceMath (Grundpreis, Rabatt), ValidityRules  │
│ region/  Berlin (PLZ), RegionalRules (Klassifikation, Sichtbarkeit)     │
│ search/  TextNormalizer, Fuzzy, ProductIdentity/Grouping, SearchEngine  │
│ data/    OfferSource-Adapter, OfferRepository (Cache, Retry, Status),   │
│          JsonFileStore, UserDataRepository, SourceCatalog               │
└────────────────────────────────────────────────────────────────────────┘
desktop/ = zweiter Host für shared-ui (Vorschau, Screenshots), wird nicht ausgeliefert
```

Abhängigkeiten zeigen nur nach unten. `core` kennt weder Android noch Compose und ist vollständig auf der JVM testbar.

## Datenfluss

1. `OfferRepository.refresh()` ruft alle **aktivierten** Quellen parallel ab, die älter als ihr Intervall sind.
2. Jeder Adapter (`OfferSource`) liefert `Success(offers)` oder `Failure(reason)`; Ausnahmen werden abgefangen.
   Ein Fehler betrifft nur diese Quelle.
3. `OfferValidator.sanitize` entfernt Unplausibles (leerer Titel, Preis ≤ 0, Zeitraum verkehrt) und streicht
   „Statt“-Preise, die nicht höher als der aktuelle Preis sind.
4. Erfolgreiche Ergebnisse werden je Quelle als JSON gespeichert (`cache_<quelle>.json`). Bei Fehlern bleibt der
   letzte Stand mit seinem Zeitstempel sichtbar, die Quelle steht auf „Fehler“.
5. `DataState` (StateFlow) enthält alle Berliner Angebote, Quellenstatus und Refresh-Zustand.
   Angebote mit `OUTSIDE_BERLIN` gelangen nie in diesen Zustand.
6. Die UI berechnet Suche, Gruppierung und Sortierung aus `DataState` mit `SearchEngine`.

## Datenmodell (Auszug)

* **Offer**: ein Preis eines Produkts bei einem Händler. Felder: `price` (Cents, nullable), `referencePrice`
  (mit Art: Streichpreis, 30-Tage-Tiefstpreis, UVP), `sizeText` (Originaltext) und `packSize` (nur wenn
  eindeutig gelesen), `validity` oder `observedOn`, `kind` (OFFER oder OBSERVATION), `availability` und `stores`,
  `conditions` (App-Preis, Pfand …), `sourceId`, `sourceUrl`, `fetchedAt`, `isSample`.
* **Regionale Gültigkeit getrennt vom Produkt**: `availability` ∈ {BERLIN_WIDE, STORE_SPECIFIC,
  NATIONAL_CONFIRMED, UNCONFIRMED, OUTSIDE_BERLIN} plus konkrete `stores`. Ein Filialpreis gilt nie
  für die ganze Kette.
* **Produktidentität** (`ProductIdentity`): Marke + bedeutungstragende Wörter des Titels, ohne Verpackungs-
  und Mengenwörter, bzw. EAN. „Ultra White“ und „Ultra Paradise“ bleiben getrennt. Verschiedene
  Packungsgrößen desselben Produkts teilen eine Gruppe, werden aber nur über den Grundpreis verglichen.
* **Fairer Preissieger** (`ProductGroup.cheapestComparable`): gleiche Packung → niedrigster Preis;
  verschiedene Packungen → niedrigster Grundpreis, nur wenn alle einen Grundpreis derselben Dimension
  haben; sonst kein Sieger.
* **Region**: `Region`-Interface mit `Berlin` als einziger Implementierung. Eine weitere Stadt wäre eine
  weitere Implementierung plus Quellen-Parameter, ohne Umbau des Modells.

## Lokal vs. Server

Alles läuft **auf dem Gerät**; es gibt **kein Backend**. Begründung in DECISIONS.md (D-05). Abrufe gehen
direkt vom Handy an die jeweilige Quelle, nur wenn die Quelle aktiviert ist.

## Speicherung

`JsonFileStore`: eine JSON-Datei pro Schlüssel im App-Verzeichnis, atomar geschrieben (Temp-Datei + Rename),
kaputte Dateien werden beiseitegelegt statt die App abstürzen zu lassen. Gespeichert werden Einstellungen,
Merkliste, Suchverlauf und der Cache je Quelle. Cloud-Backup ist ausgeschlossen (`allowBackup=false`).

## UI-Schichten und Glas

```
Ebene 3  Filter-Sheet (Glas mit Unschärfe), Scrim
Ebene 2  schwebende Leisten: Suchkopf, Detail-Kopf, Navigation (Glas mit Unschärfe)
Ebene 1  Aurora-Hintergrund + scrollender Inhalt  ← Quelle der Unschärfe (Haze hazeSource)
```

Unschärfe gibt es nur für Ebene 2 und 3 und nur ab Android 12 (RenderEffect via Haze). Darunter, im
Desktop-Host und bei „Transparenz reduzieren“ werden dichtere, fast deckende Flächen verwendet.
Karten im Inhalt sind leicht transparent ohne Unschärfe, damit Text immer lesbar bleibt.

## Hintergrundarbeit und Preisalarme (geplant, Phase 4)

* WorkManager, `PeriodicWorkRequest` alle 12 h, Constraint „Netzwerk verbunden“, Akku nicht niedrig.
* Benachrichtigungen erst nach Abfrage von `POST_NOTIFICATIONS` (Android 13+).
* Ein Alarm ist nur auswählbar, wenn mindestens eine aktivierte, nicht experimentelle Quelle das Produkt
  regelmäßig liefern kann, und prüft nur Angebote mit bestätigter Berliner Gültigkeit (oder kennzeichnet sie).
* Bis dahin zeigt die App „Preisalarm in Vorbereitung“ und speichert keine Alarme.

## Fehlerprotokolle

`OfferRepository` protokolliert Quelle, Versuch und Fehlerart (Android: Logcat-Tag `Kiezpreis`).
Suchbegriffe, Postleitzahl und Merkliste werden nie protokolliert.
