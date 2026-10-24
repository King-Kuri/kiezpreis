# Kiezpreis

Android-App zum Suchen und Vergleichen aktueller Supermarktpreise **in Berlin**.
Kotlin, Jetpack Compose, Material 3, eigenständige „Liquid Glass“-Gestaltung in Dark und Light Mode.

> **Stand (Phase 2, 24.10.2026):** Lauffähiger Prototyp mit vollständiger Navigation, Suche, Filtern,
> Produktdetail, Merkliste und Einstellungen. Angezeigt werden klar gekennzeichnete **Beispieldaten**
> und, als erste echte Quelle, **Open Prices** (offene, von Nutzern erfasste Berliner Preise).
> Prospektangebote der Händler sind noch **nicht** angebunden, siehe [DATA_SOURCES.md](DATA_SOURCES.md).

## Was die App heute kann

| Bereich | Status |
|---|---|
| Startseite mit Suche, starken Angeboten, Kategorien, Verlauf, Merkliste, Händlern | fertig, getestet (Screenshots) |
| Suche: Umlaute, Bindestriche, Tippfehler, Synonyme, Varianten-Trennung | fertig, Unit-Tests |
| Filter (Händler, Kategorie, Höchstpreis, Berlin bestätigt, Rabatt, Beobachtungen) und Sortierung (Relevanz, Preis, Grundpreis, Rabatt, Endet bald) | fertig, Unit-Tests |
| Grundpreis- und Rabattberechnung nur mit belegten Daten | fertig, Unit-Tests |
| Berliner Gültigkeit in vier Klassen, Ausschluss von Preisen außerhalb Berlins | fertig, Unit-Tests |
| Produktdetail mit fairem Preissieger, Packungsgrößen, Filialen, Quelle, Zeitraum | fertig |
| Merkliste (lokal), Suchverlauf, Postleitzahl (nur Berlin), bevorzugte Händler, Theme, Bewegung/Transparenz reduzieren | fertig, Persistenz getestet |
| Datenquellen einzeln an/aus, Status, Verbindungstest, Cache mit Offline-Stand | fertig, Unit-Tests |
| Open Prices als echte Quelle | implementiert, mit Testdaten geprüft; Live-Abruf nur auf dem Gerät möglich (siehe unten) |
| Preisalarme | **nicht aktiv**, bewusst nur als „in Vorbereitung“ angezeigt |
| Händlerseiten (alle 7 Händler) | **experimentell**: Seite in der App öffnen, Angebote per Knopfdruck übernehmen und prüfen (DATA_SOURCES.md, D-17) |
| Automatischer Abruf von Händlerprospekten | **nicht angebunden**, Bewertung in DATA_SOURCES.md |

## APK installieren

Jeder Push auf `main` baut per GitHub Actions eine Debug-APK:

* **Release „debug-latest“** im Repo (Assets → `kiezpreis-debug.apk`), oder
* Actions → letzter Lauf „Build“ → Artefakt `kiezpreis-debug-apk`.

Auf dem Handy „Installation aus unbekannten Quellen“ für den Browser bzw. Dateimanager erlauben.
Mindestversion Android 8.0; echte Hintergrundunschärfe ab Android 12, darunter dichtere Glasflächen.

## Selbst bauen

Voraussetzungen: JDK 17+, Android SDK (Platform 35).

```bash
./gradlew :core:test                                   # Geschäftslogik-Tests
./gradlew -Pkiez.android=true :app:assembleDebug       # APK -> app/build/outputs/apk/debug/
./gradlew :desktop:screenshots                         # alle Screens als PNG -> desktop/build/screenshots
./gradlew :desktop:run                                 # interaktive Desktop-Vorschau
```

Ohne `-Pkiez.android=true` wird das Android-Modul nicht eingebunden; Kern und Desktop-Vorschau
bauen dann auch ohne Googles Maven-Repository (Hintergrund: [DECISIONS.md](DECISIONS.md), D-03).

## Projektstruktur

```
core/        reines Kotlin: Modelle, Preislogik, Berlin-Regeln, Suche, Quellen-Adapter, Speicher, Tests
shared-ui/   Compose-Oberfläche (Screens, Komponenten, Designsystem), von app und desktop kompiliert
app/         Android-App: Activity, Haze-Unschärfe, Insets, Back-Geste, Ressourcen
desktop/     Entwicklungs-Vorschau und Screenshot-Renderer (wird nicht ausgeliefert)
```

Weitere Dokumente: [PLAN.md](PLAN.md) · [DATA_SOURCES.md](DATA_SOURCES.md) · [ARCHITECTURE.md](ARCHITECTURE.md) · [DECISIONS.md](DECISIONS.md)

Preisdaten teilweise von [Open Prices](https://prices.openfoodfacts.org) (Open Food Facts), Lizenz ODbL 1.0.
