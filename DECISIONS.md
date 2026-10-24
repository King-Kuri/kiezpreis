# Entscheidungen und Annahmen

Jede Entscheidung ist austauschbar; Begründung und Alternative stehen dabei.

**D-01 · App-Name „Kiezpreis“.** Berlin-Bezug („Kiez“), sagt was die App tut, kurz. Vorläufig.

**D-02 · Native Android mit Kotlin, Jetpack Compose, Material 3.** Wie gewünscht. minSdk 26 (Android 8,
deckt praktisch alle Geräte ab und hat `java.time`), targetSdk 35. Keine DI-Bibliothek: das Wiring passt in
eine Klasse (`KiezApplication`).

**D-03 · Geteilte UI-Quellen und Desktop-Vorschau.** Der Cloud-Container, in dem entwickelt wird, erreicht
Googles Maven-Repository und `dl.google.com` nicht (Netzwerkrichtlinie). Ohne diese gibt es kein Android
Gradle Plugin und keine AndroidX-Bibliotheken. Lösung:
* `core` ist reines Kotlin und wird lokal gebaut und getestet.
* Die Compose-Oberfläche liegt in `shared-ui/src` und wird zweimal kompiliert: von `app` gegen AndroidX
  Compose (BOM 2025.05.01) und von `desktop` gegen Compose Multiplatform 1.5.12. Das ist die neueste
  Desktop-Version, deren Abhängigkeiten vollständig auf Maven Central liegen. Die Oberfläche nutzt deshalb
  nur APIs, die in beiden Versionen existieren.
* `desktop` rendert alle Screens als PNG (Dark/Light), damit Gestaltung ohne Gerät geprüft werden kann.
* Die echte APK baut GitHub Actions. Mit einer Cloud-Umgebung, die `dl.google.com` erlaubt, ginge es auch lokal.
Alternative: reines Android-Projekt ohne Desktop-Host. Dann wäre UI-Prüfung nur in CI möglich.

**D-04 · Speicherung als JSON-Dateien statt Room.** Room hängt von Google Maven ab und wäre im Container
nicht testbar. Die Datenmengen (Merkliste, Verlauf, einige Hundert bis Tausend Angebote pro Quelle) sind
für atomar geschriebene JSON-Dateien klein genug. Die Speicherschicht steckt hinter Repositories; ein
Wechsel zu Room oder SQLDelight betrifft nur `data/`.

**D-05 · Kein Backend im MVP.** Die einzige zulässige Live-Quelle (Open Prices) ist eine öffentliche API ohne
Schlüssel, die direkt vom Gerät abgefragt werden kann. Ein Server würde Kosten, Wartung und eine
Weitergabe von Händlerdaten an Dritte bedeuten. Ein Backend wird erst sinnvoll, wenn Händlerquellen
serverseitig gesammelt werden sollen; dann günstig als geplanter GitHub-Actions-Job oder kleiner
Cloud-Function-Dienst. Das wäre eine eigene Entscheidung (Rechtslage!).

**D-06 · Keine Umgehung von Schutzmaßnahmen.** Keine Client-Zertifikate oder Schlüssel aus Händler-Apps,
keine Headless-Browser gegen Bot-Schutz, keine Abrufe gegen robots.txt. Daraus folgt: REWE, EDEKA,
PENNY, Kaufland und Netto sind vorerst nicht angebunden (DATA_SOURCES.md).

**D-07 · Beispieldaten statt Leere.** Für die Gestaltungsphase gibt es 40 erfundene Beispielpreise. Sie sind
an jeder Karte als „Beispiel“ markiert, haben keine Quell-Links, fiktive Filialnamen und ein Banner auf
Start- und Suchseite. Sie lassen sich in den Einstellungen abschalten. Die Datumsangaben liegen in der
laufenden Woche, damit Gültigkeitsanzeigen sichtbar sind; das ist im Banner offengelegt.

**D-08 · Vier Berlin-Klassen plus „außerhalb“.** Wie im Auftrag verlangt; zusätzlich `OUTSIDE_BERLIN`, damit
ein Brandenburger Preis nicht nur „unbestätigt“, sondern sicher ausgeblendet wird. „Unbestätigt“ wird
angezeigt (bernsteinfarben gekennzeichnet) und lässt sich per Filter ausblenden.

**D-09 · Berlin über Postleitzahl, nicht über Umkreis.** Ein 25-km-Kreis enthält Potsdam, Teltow, Dahlewitz usw.
PLZ-Bereiche mit Ausnahmeliste sind eindeutig und offline prüfbar.

**D-10 · Grundpreis nur aus eindeutig gelesener Packungsgröße.** Bereiche („400–500 g“), mehrere Größen
oder fehlende Angaben ergeben „Grundpreis unbekannt“. Grundpreis je kg, l oder Stück (PAngV), kaufmännisch
gerundet. Den vom Händler gedruckten Grundpreis bewahren wir als Text (`statedBasePrice`) für Transparenz.

**D-11 · Rabatt nur mit belegtem Vergleichspreis, abgerundet.** Die Art des Vergleichspreises (Streichpreis,
30-Tage-Tiefstpreis, UVP) wird mit angezeigt. 28,8 % werden als 28 % gezeigt, nie aufgerundet.

**D-12 · Preisbeobachtungen sind keine Angebote.** Open-Prices-Daten haben ein Beobachtungsdatum statt eines
Angebotszeitraums. Sie werden als „Preisbeobachtung“ markiert, gelten 21 Tage als aktuell und können per
Filter ausgeblendet werden.

**D-13 · Produktgruppen konservativ.** Lieber zwei getrennte Treffer als ein falsch zusammengelegter.
Gleiche EAN = gleiches Produkt; sonst Marke plus alle bedeutungstragenden Wörter.

**D-14 · Glas sparsam.** Echte Unschärfe nur für schwebende Leisten und Sheets (Haze 1.6, Android 12+).
Inhaltskarten sind leicht transparent ohne Unschärfe. Bei „Transparenz reduzieren“ wird alles deckend.
Animationen 180–280 ms, bei „Bewegung reduzieren“ Überblendungen ohne Bewegung; der Aurora-Hintergrund steht dann still.

**D-15 · Keine Schriftdateien eingebettet.** Systemschrift (Roboto) mit eigener Typo-Skala und tabellarischen
Ziffern für Preise. Spart APK-Größe und Lizenzfragen; eine eigene Schrift kann später ergänzt werden.

**D-16 · Händlerkennzeichen sind keine Logos.** Farbige Wortmarken in Markenfarbe, um keine Markenlogos
nachzubilden.

**D-17 · Händlerseiten nur sichtbar und auf Knopfdruck (Noel, 24.10.2026).** Für REWE, EDEKA, PENNY,
Kaufland, Netto, Lidl und ALDI Nord öffnet die App die Händlerseite in einer WebView. Die Person wählt dort
selbst ihren Markt und beantwortet Cookie- oder Sicherheitsabfragen selbst. Erst nach Tippen auf
„Angebote übernehmen“ liest die App die Seite. Sie liest nur, was schon da ist: JSON-LD, eingebettetes JSON
und die JSON-Antworten, die die Seite selbst geladen hat. Dazu kommen Produktkacheln, in denen genau ein Preis steht.
Keine Hintergrundabrufe, kein automatisches Klicken, keine Umgehung von Bot-Schutz, keine Weitergabe.
Navigation bleibt auf der Domain des Händlers. Eine JavaScript-Brücke gibt es nicht. Das Ergebnis kommt über
`evaluateJavascript` zurück, und die URL setzt die App selbst ein.
Begründung: So entspricht die Nutzung dem privaten Lesen der Seite. Die REWE-App-Bedingungen (Stand
13.06.2023) erlauben nur private Nutzung, und genau das bleibt es. Bot-Schutz wird nicht umgangen, weil ein
Mensch die Seite bedient.

**D-18 · Vorschau vor dem Speichern, Markt durch die Person.** Erkannte Preise werden erst nach Sichtprüfung
übernommen, jeder einzeln abwählbar. Filialgenau („Nur 1 Filiale“) wird ein Preis nur, wenn eine Berliner PLZ
für den Markt angegeben ist. Eine PLZ, die die Seite nennt, wird nur vorgeschlagen. Ohne Markt heißt es
„Berlin nicht bestätigt“, und eine PLZ außerhalb Berlins wird abgelehnt. Mehrdeutiges wird verworfen, etwa
Ganzzahlen ohne Cent-Hinweis, mehrere Preise in einer Kachel oder Fremdwährung. Preise ohne Enddatum
verschwinden nach 7 Tagen. Ein neuer Import für denselben Händler und Markt ersetzt den alten.

**D-19 · Alle Angebote statt nur Berlin (Noel, 24.10.2026).** Die Region der Person filtert nicht mehr.
Jedes Angebot trägt sein Gebiet (`Coverage`), und die App zeigt es an. Um den Preisvergleich ehrlich zu
halten, konkurrieren nur bundesweite, regionale und Angebote ohne Gebietsangabe um „Bester Preis“ und
„Günstigstes“. Preise einzelner Märkte stehen in einem eigenen Abschnitt „Einzelne Märkte“ mit Ort. Sonst
wäre ein Regalpreis aus einer anderen Stadt oft der Sieger. Optional gibt es „Nur meine Gegend“ (PLZ).
Regionale Angebote zählen mit zum Vergleich, weil sie für die Kette im ganzen Gebiet gelten; ihr Gebiet
steht dabei. ALDI ist in ALDI Nord und ALDI Süd getrennt. Alte Caches werden weiter gelesen.

## Annahmen

* Berlin liegt im ALDI-Nord-Gebiet; EDEKA Berlin gehört zur Region Minden-Hannover.
* „Netto“ ohne Zusatz in OpenStreetMap ist mehrdeutig und wird nicht zugeordnet; nur „Netto Marken-Discount“.
* Die Postleitzahl des Nutzers verbessert die Ergebnisse erst, wenn eine filialgenaue Quelle sie nutzt;
  das wird in den Einstellungen so gesagt.
