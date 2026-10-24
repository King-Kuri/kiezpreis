package de.kiezpreis.core

import de.kiezpreis.core.logic.PriceMath
import de.kiezpreis.core.model.Coverage
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.MeasureUnit
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PackSize
import de.kiezpreis.core.model.ReferencePrice
import de.kiezpreis.core.model.ReferencePriceKind
import de.kiezpreis.core.model.Retailer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceMathTest {
    @Test fun `base price per liter for a can`() {
        val up = PriceMath.unitPrice(Cents(99), PackSize(1, 0.25, MeasureUnit.LITER))!!
        assertEquals(Cents(396), up.cents)
        assertEquals("3,96 €/l", up.format())
    }

    @Test fun `base price of a multipack uses total content`() {
        // 20 x 0.5 l for 8,99 € = 0,90 €/l (as printed by REWE)
        assertEquals(Cents(90), PriceMath.unitPrice(Cents(899), PackSize(20, 0.5, MeasureUnit.LITER))!!.cents)
    }

    @Test fun `base price per kg from grams rounds half up`() {
        // 3,79 € / 420 g = 9,0238 €/kg -> 9,02 €
        assertEquals(Cents(902), PriceMath.unitPrice(Cents(379), PackSize(1, 420.0, MeasureUnit.GRAM))!!.cents)
        // 1,69 € / 140 g = 12,071 -> 12,07 €
        assertEquals(Cents(1207), PriceMath.unitPrice(Cents(169), PackSize(1, 140.0, MeasureUnit.GRAM))!!.cents)
    }

    @Test fun `price per piece`() =
        assertEquals("0,45 €/Stück", PriceMath.unitPrice(Cents(449), PackSize(10, 1.0, MeasureUnit.PIECE))!!.format())

    @Test fun `no base price without size or price`() {
        assertNull(PriceMath.unitPrice(Cents(100), null))
        assertNull(PriceMath.unitPrice(null, PackSize(1, 1.0, MeasureUnit.LITER)))
    }

    @Test fun `discount only with evidenced higher reference price and rounded down`() {
        assertEquals(28, PriceMath.discountPercent(Cents(99), Cents(139))) // 28.78 -> 28
        assertNull(PriceMath.discountPercent(Cents(139), Cents(139)))
        assertNull(PriceMath.discountPercent(Cents(150), Cents(139)))
        val offer = Offer(id = "x", sourceId = "t", retailer = Retailer.LIDL, title = "x", price = Cents(99),
            availability = Coverage.REGIONAL, fetchedAt = 0)
        assertNull(PriceMath.discountPercent(offer))
        assertEquals(28, PriceMath.discountPercent(offer.copy(referencePrice = ReferencePrice(Cents(139), ReferencePriceKind.STRIKE_THROUGH))))
        assertNull(PriceMath.discountPercent(offer.copy(price = null, referencePrice = ReferencePrice(Cents(139), ReferencePriceKind.STRIKE_THROUGH))))
    }

    @Test fun `cents formatting`() {
        assertEquals("0,05 €", Cents(5).format())
        assertEquals("12,00 €", Cents(1200).format())
        assertEquals(Cents(129), Cents.ofEuro(1.29))
    }
}
