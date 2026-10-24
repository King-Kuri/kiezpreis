package de.kiezpreis.core

import de.kiezpreis.core.logic.ValidityRules
import de.kiezpreis.core.logic.ValidityStatus
import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PriceKind
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Validity
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidityTest {
    private val today = LocalDate.of(2026, 10, 24)

    @Test fun statuses() {
        assertEquals(ValidityStatus.ACTIVE, ValidityRules.status(Validity("2026-10-19", "2026-10-25"), today))
        assertEquals(ValidityStatus.ACTIVE, ValidityRules.status(Validity("2026-10-19", "2026-10-24"), today))
        assertEquals(ValidityStatus.EXPIRED, ValidityRules.status(Validity("2026-10-12", "2026-10-23"), today))
        assertEquals(ValidityStatus.UPCOMING, ValidityRules.status(Validity("2026-10-26", "2026-10-31"), today))
        assertEquals(ValidityStatus.UNKNOWN, ValidityRules.status(Validity(), today))
        assertEquals(ValidityStatus.UNKNOWN, ValidityRules.status(Validity("kaputt", null), today))
    }

    @Test fun labels() {
        val o = Offer(id = "1", sourceId = "t", retailer = Retailer.LIDL, title = "x", price = Cents(1),
            availability = Coverage.REGIONAL, fetchedAt = 0)
        assertEquals("nur noch heute", ValidityRules.label(o.copy(validity = Validity("2026-10-19", "2026-10-24")), today))
        assertEquals("bis morgen", ValidityRules.label(o.copy(validity = Validity("2026-10-19", "2026-10-25")), today))
        assertEquals("ab Mo 26.10.", ValidityRules.label(o.copy(validity = Validity("2026-10-26", "2026-10-31")), today))
        assertEquals("abgelaufen", ValidityRules.label(o.copy(validity = Validity("2026-10-12", "2026-10-17")), today))
    }

    @Test fun `observations expire after three weeks`() {
        val o = Offer(id = "1", sourceId = "t", retailer = Retailer.LIDL, title = "x", price = Cents(1), kind = PriceKind.OBSERVATION,
            availability = Coverage.STORE, fetchedAt = 0)
        assertEquals(ValidityStatus.ACTIVE, ValidityRules.status(o.copy(observedOn = "2026-10-10"), today))
        assertEquals(ValidityStatus.EXPIRED, ValidityRules.status(o.copy(observedOn = "2026-09-01"), today))
        assertEquals(ValidityStatus.UNKNOWN, ValidityRules.status(o.copy(observedOn = null), today))
        assertEquals("gestern gesehen", ValidityRules.label(o.copy(observedOn = "2026-10-23"), today))
    }
}
