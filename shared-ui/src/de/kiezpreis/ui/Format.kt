package de.kiezpreis.ui

import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Store
import de.kiezpreis.core.model.Offer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val BerlinZone: ZoneId = ZoneId.of("Europe/Berlin")

object Labels {
    fun availabilityShort(offer: Offer): String = when (offer.availability) {
        Coverage.NATIONAL -> "Bundesweit"
        Coverage.REGIONAL -> offer.area ?: "Regional"
        Coverage.STORE -> when (offer.stores.size) {
            0 -> "Nur 1 Markt"
            1 -> "Nur 1 Markt" + (place(offer.stores.single())?.let { " · $it" } ?: "")
            else -> "Nur ${offer.stores.size} Märkte"
        }
        Coverage.UNKNOWN -> "Gebiet unbekannt"
    }

    fun availabilityLong(offer: Offer): String = when (offer.availability) {
        Coverage.NATIONAL ->
            "Laut Quelle in allen ${offer.retailer.shortName}-Filialen in Deutschland."
        Coverage.REGIONAL ->
            "Gilt laut Quelle nur im Gebiet „${offer.area ?: "unbenannt"}“. Anderswo kann der Preis abweichen oder das Angebot fehlen."
        Coverage.STORE ->
            "Preis nur für den genannten Markt belegt. Andere ${offer.retailer.shortName}-Märkte können abweichen. " +
                "Zählt deshalb nicht beim Vergleich des besten Preises."
        Coverage.UNKNOWN ->
            "Die Quelle sagt nicht, wo das Angebot gilt. Vor dem Einkauf prüfen."
    }

    /** "Berlin", "München" or the postal code. */
    fun place(store: Store): String? = store.city?.takeIf { it.isNotBlank() } ?: store.postalCode

    private val time = DateTimeFormatter.ofPattern("HH:mm")
    private val dayMonth = DateTimeFormatter.ofPattern("dd.MM.")

    /** "heute 14:32", "gestern 09:10", "21.10. 08:00" */
    fun timestamp(millis: Long, today: LocalDate): String {
        val dt = Instant.ofEpochMilli(millis).atZone(BerlinZone)
        val days = ChronoUnit.DAYS.between(dt.toLocalDate(), today)
        return when (days) {
            0L -> "heute ${dt.format(time)}"
            1L -> "gestern ${dt.format(time)}"
            else -> "${dt.format(dayMonth)} ${dt.format(time)}"
        }
    }

    fun count(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"
}
