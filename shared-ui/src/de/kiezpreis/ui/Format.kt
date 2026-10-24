package de.kiezpreis.ui

import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.Offer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val BerlinZone: ZoneId = ZoneId.of("Europe/Berlin")

object Labels {
    fun availabilityShort(offer: Offer): String = when (offer.availability) {
        BerlinAvailability.BERLIN_WIDE -> "Berlin"
        BerlinAvailability.STORE_SPECIFIC -> when (offer.stores.size) {
            0, 1 -> "Nur 1 Filiale"
            else -> "Nur ${offer.stores.size} Filialen"
        }
        BerlinAvailability.NATIONAL_CONFIRMED -> "Bundesweit inkl. Berlin"
        BerlinAvailability.UNCONFIRMED -> "Berlin nicht bestätigt"
        BerlinAvailability.OUTSIDE_BERLIN -> "Außerhalb Berlins"
    }

    fun availabilityLong(offer: Offer): String = when (offer.availability) {
        BerlinAvailability.BERLIN_WIDE ->
            "Gilt laut Quelle in allen oder mehreren Berliner Filialen von ${offer.retailer.shortName}."
        BerlinAvailability.STORE_SPECIFIC ->
            "Preis nur für die genannte Filiale belegt. Andere ${offer.retailer.shortName}-Filialen können abweichen."
        BerlinAvailability.NATIONAL_CONFIRMED ->
            "Bundesweit beworben, laut Quelle in allen Filialen, also auch in Berlin."
        BerlinAvailability.UNCONFIRMED ->
            "Die Quelle sagt nicht, ob Berliner Filialen teilnehmen. Vor dem Einkauf prüfen."
        BerlinAvailability.OUTSIDE_BERLIN -> "Nicht in Berlin gültig."
    }

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
