package de.kiezpreis.core.logic

import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Dimension
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.PackSize
import java.math.BigDecimal
import java.math.RoundingMode

/** Base price ("Grundpreis") such as 3,96 €/l. */
data class UnitPrice(val cents: Cents, val per: String, val dimension: Dimension) : Comparable<UnitPrice> {
    override fun compareTo(other: UnitPrice): Int {
        require(dimension == other.dimension) { "Cannot compare $dimension with ${other.dimension}" }
        return cents.compareTo(other.cents)
    }

    fun format(): String = "${cents.format()}/$per"
}

object PriceMath {

    /**
     * Base price per 1 kg, 1 l or 1 Stück, as § 4 PAngV prescribes for food.
     * Returns null when price or pack size is unknown: never estimated.
     */
    fun unitPrice(price: Cents?, pack: PackSize?): UnitPrice? {
        if (price == null || pack == null || price.value <= 0) return null
        val total = pack.totalInBaseUnit
        if (total <= 0.0) return null
        val (perQuantity, label) = when (pack.dimension) {
            Dimension.MASS -> 1000.0 to "kg"
            Dimension.VOLUME -> 1000.0 to "l"
            Dimension.COUNT -> 1.0 to "Stück"
        }
        val cents = BigDecimal(price.value)
            .multiply(BigDecimal.valueOf(perQuantity))
            .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
        return UnitPrice(Cents(cents.toLong()), label, pack.dimension)
    }

    fun unitPrice(offer: Offer): UnitPrice? = unitPrice(offer.price, offer.packSize)

    /**
     * Discount in whole percent, rounded down so it is never overstated.
     * Only computed against a reference price the source actually publishes.
     */
    fun discountPercent(offer: Offer): Int? {
        val price = offer.price ?: return null
        val reference = offer.referencePrice?.price ?: return null
        return discountPercent(price, reference)
    }

    fun discountPercent(price: Cents, reference: Cents): Int? {
        if (reference.value <= 0 || price.value <= 0 || price >= reference) return null
        val pct = (reference.value - price.value) * 100 / reference.value
        return pct.toInt().takeIf { it > 0 }
    }
}
