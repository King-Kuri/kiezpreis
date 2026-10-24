package de.kiezpreis.core.logic

import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PriceKind
import de.kiezpreis.core.model.Validity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class ValidityStatus { UPCOMING, ACTIVE, EXPIRED, UNKNOWN }

object ValidityRules {

    /** Observations older than this are no longer treated as a current price. */
    const val OBSERVATION_MAX_AGE_DAYS = 21L

    fun parse(date: String?): LocalDate? = date?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

    fun status(validity: Validity, today: LocalDate): ValidityStatus {
        val from = parse(validity.from)
        val to = parse(validity.to)
        return when {
            from == null && to == null -> ValidityStatus.UNKNOWN
            to != null && today.isAfter(to) -> ValidityStatus.EXPIRED
            from != null && today.isBefore(from) -> ValidityStatus.UPCOMING
            else -> ValidityStatus.ACTIVE
        }
    }

    fun status(offer: Offer, today: LocalDate): ValidityStatus = when (offer.kind) {
        PriceKind.OFFER -> status(offer.validity, today)
        PriceKind.OBSERVATION -> {
            val seen = parse(offer.observedOn)
            when {
                seen == null -> ValidityStatus.UNKNOWN
                seen.isAfter(today) -> ValidityStatus.UNKNOWN
                ChronoUnit.DAYS.between(seen, today) > OBSERVATION_MAX_AGE_DAYS -> ValidityStatus.EXPIRED
                else -> ValidityStatus.ACTIVE
            }
        }
    }

    /**
     * Whether an offer is listed at all: active, upcoming, or with unknown dates (then labelled
     * "Zeitraum unbekannt"). Expired offers and outdated observations never count as current.
     */
    fun isCurrent(offer: Offer, today: LocalDate): Boolean = status(offer, today) != ValidityStatus.EXPIRED

    private val short = DateTimeFormatter.ofPattern("dd.MM.")

    /** Human readable label, e.g. "gültig bis Sa 25.10." */
    fun label(offer: Offer, today: LocalDate): String {
        if (offer.kind == PriceKind.OBSERVATION) {
            val seen = parse(offer.observedOn) ?: return "Datum unbekannt"
            val days = ChronoUnit.DAYS.between(seen, today)
            return when (days) {
                0L -> "heute gesehen"
                1L -> "gestern gesehen"
                else -> "gesehen am ${seen.format(short)}"
            }
        }
        val from = parse(offer.validity.from)
        val to = parse(offer.validity.to)
        return when (status(offer.validity, today)) {
            ValidityStatus.UNKNOWN -> "Zeitraum unbekannt"
            ValidityStatus.EXPIRED -> "abgelaufen"
            ValidityStatus.UPCOMING -> "ab ${weekday(from!!)} ${from.format(short)}"
            ValidityStatus.ACTIVE -> when {
                to == null -> "seit ${from!!.format(short)}"
                to == today -> "nur noch heute"
                ChronoUnit.DAYS.between(today, to) == 1L -> "bis morgen"
                else -> "bis ${weekday(to)} ${to.format(short)}"
            }
        }
    }

    fun periodLabel(validity: Validity): String? {
        val from = parse(validity.from)
        val to = parse(validity.to)
        return when {
            from != null && to != null -> "${from.format(short)} – ${to.format(short)}${to.year}"
            to != null -> "bis ${to.format(short)}${to.year}"
            from != null -> "ab ${from.format(short)}${from.year}"
            else -> null
        }
    }

    private fun weekday(date: LocalDate): String =
        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")[date.dayOfWeek.value - 1]
}
