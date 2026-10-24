package de.kiezpreis.core

import de.kiezpreis.core.logic.PackSizeParser
import de.kiezpreis.core.model.MeasureUnit
import de.kiezpreis.core.model.PackSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PackSizeParserTest {
    @Test fun `REWE style can with base price in parentheses`() =
        assertEquals(PackSize(1, 0.25, MeasureUnit.LITER), PackSizeParser.parse("je 0,25-l-Dose, (1 l = 3,96 €), zzgl. 0,25 € Pfand"))

    @Test fun `crate with multiplier`() =
        assertEquals(PackSize(20, 0.5, MeasureUnit.LITER), PackSizeParser.parse("je 20 x 0,5-l-Fl.-Kasten, (1 l = 0,90 €)"))

    @Test fun `multipack with unicode times`() =
        assertEquals(PackSize(6, 1.5, MeasureUnit.LITER), PackSizeParser.parse("6 × 1,5 l"))

    @Test fun `grams and kilograms`() {
        assertEquals(PackSize(1, 500.0, MeasureUnit.GRAM), PackSizeParser.parse("500 g"))
        assertEquals(PackSize(1, 1.2, MeasureUnit.KILOGRAM), PackSizeParser.parse("1,2 kg"))
        assertEquals(PackSize(1, 420.0, MeasureUnit.GRAM), PackSizeParser.parse("je 420-g-Pckg., (1 kg = 9,02 €)"))
    }

    @Test fun `pieces`() = assertEquals(PackSize(10, 1.0, MeasureUnit.PIECE).totalInBaseUnit, PackSizeParser.parse("10 Stück")!!.totalInBaseUnit)

    @Test fun `centiliter converts to milliliter`() = assertEquals(330.0, PackSizeParser.parse("33 cl")!!.totalInBaseUnit)

    @Test fun `unknown or ambiguous sizes give null`() {
        assertNull(PackSizeParser.parse(null))
        assertNull(PackSizeParser.parse(""))
        assertNull(PackSizeParser.parse("je Packung"))
        assertNull(PackSizeParser.parse("Stück"))
        assertNull(PackSizeParser.parse("400-500 g"))
        assertNull(PackSizeParser.parse("versch. Sorten, 200 g oder 300 g"))
        assertNull(PackSizeParser.parse("1,5 Stück"))
    }

    @Test fun `same size mentioned twice is not ambiguous`() =
        assertEquals(PackSize(1, 250.0, MeasureUnit.GRAM), PackSizeParser.parse("250 g Packung, 250 g"))

    @Test fun `words starting with unit letters are not units`() = assertNull(PackSizeParser.parse("2 Gläser"))
}
