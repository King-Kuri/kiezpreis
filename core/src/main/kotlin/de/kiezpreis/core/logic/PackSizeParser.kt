package de.kiezpreis.core.logic

import de.kiezpreis.core.model.MeasureUnit
import de.kiezpreis.core.model.PackSize

/**
 * Reads package sizes from the free text retailers print, e.g.
 * "je 0,25-l-Dose", "6 x 0,33 l", "20 x 0,5-l-Fl.-Kasten", "500 g", "1 kg", "60 Stück".
 *
 * Returns null whenever the text is ambiguous (several different sizes, ranges such as
 * "400-500 g", or "je ... Packung" without amount). A wrong size would produce a wrong base
 * price, so the parser prefers "unknown" over guessing.
 */
object PackSizeParser {

    private const val NUM = """(\d+(?:[.,]\d+)?)"""
    private const val UNIT = """(kg|g|gr|mg|l|ltr|liter|ml|cl|stück|stk|st\.|stck)"""

    // Unit must not be followed by a letter, except for the "-l-Dose" style hyphen.
    private val multi = Regex("""(\d+)\s*[x×]\s*$NUM\s*-?\s*$UNIT(?![a-zäöü])""", RegexOption.IGNORE_CASE)
    private val single = Regex("""(?<![\d,.])$NUM\s*-?\s*$UNIT(?![a-zäöü])""", RegexOption.IGNORE_CASE)
    private val range = Regex("""\d+(?:[.,]\d+)?\s*(?:-|–|bis)\s*\d+(?:[.,]\d+)?\s*-?\s*$UNIT(?![a-zäöü])""", RegexOption.IGNORE_CASE)

    // Everything inside parentheses is usually the base price "(1 l = 3,96 €)" and must be ignored.
    private val parenthesised = Regex("""\([^)]*\)""")
    private val basePriceFragment = Regex("""\d+\s*(?:kg|l|g|ml)\s*=\s*[\d.,]+\s*€?""", RegexOption.IGNORE_CASE)

    fun parse(text: String?): PackSize? {
        if (text.isNullOrBlank()) return null
        val cleaned = text
            .replace(parenthesised, " ")
            .replace(basePriceFragment, " ")
            .replace(' ', ' ')
        if (range.containsMatchIn(cleaned)) return null

        val multis = multi.findAll(cleaned).toList()
        if (multis.isNotEmpty()) {
            val sizes = multis.mapNotNull { m ->
                val count = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                build(count, m.groupValues[2], m.groupValues[3])
            }.distinct()
            return sizes.singleOrNull()
        }

        val singles = single.findAll(cleaned).mapNotNull { m ->
            build(1, m.groupValues[1], m.groupValues[2])
        }.distinct().toList()
        return singles.singleOrNull()
    }

    private fun build(count: Int, numberText: String, unitText: String): PackSize? {
        val number = numberText.replace(',', '.').toDoubleOrNull() ?: return null
        if (number <= 0 || count < 1) return null
        val (amount, unit) = when (unitText.lowercase()) {
            "g", "gr" -> number to MeasureUnit.GRAM
            "mg" -> number / 1000.0 to MeasureUnit.GRAM
            "kg" -> number to MeasureUnit.KILOGRAM
            "ml" -> number to MeasureUnit.MILLILITER
            "cl" -> number * 10 to MeasureUnit.MILLILITER
            "l", "ltr", "liter" -> number to MeasureUnit.LITER
            "stück", "stk", "st.", "stck" -> {
                if (number % 1.0 != 0.0) return null
                number to MeasureUnit.PIECE
            }
            else -> return null
        }
        return PackSize(count = count, amountPerItem = amount, unit = unit)
    }
}
