package com.radixdlt.client.assets

import java.math.BigDecimal

/**
 * Class mainly for formatting amounts in error messages and other English text.
 */
class Amount(val asset: Asset, val amountInSubunits: Long) {

    fun getTokenClass(): Asset {
        return asset
    }

    fun lte(amount: Amount): Boolean {
        if (amount.getTokenClass() != this.getTokenClass()) {
            throw IllegalArgumentException("Only amounts with the same token class can be compared.")
        }

        return this.amountInSubunits <= amount.amountInSubunits
    }

    fun gte(amount: Amount): Boolean {
        if (amount.getTokenClass() != this.getTokenClass()) {
            throw IllegalArgumentException("Only amounts with the same token class can be compared.")
        }

        return this.amountInSubunits >= amount.amountInSubunits
    }

    override fun hashCode(): Int {
        // Hackish but good for now
        return this.toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Amount) {
            return other.amountInSubunits == this.amountInSubunits && other.asset == this.asset
        }

        return false
    }

    override fun toString(): String {
        return "${formattedAmount()} ${asset.iso}"
    }

    private fun formattedAmount(): String {
        val remainder = amountInSubunits % asset.subUnits

        if (remainder == 0L) {
            // Whole number
            return (amountInSubunits / asset.subUnits).toString()
        }

        if (isPowerOfTen(asset.subUnits)) {
            // Decimal format
            return BigDecimal.valueOf(amountInSubunits).divide(BigDecimal.valueOf(asset.subUnits.toLong())).toString()
        }

        // Fraction format
        val quotient = amountInSubunits / asset.subUnits
        val fraction = remainder.toString() + "/" + asset.subUnits
        return if (quotient == 0L) {
            fraction
        } else quotient.toString() + " and " + fraction
    }

    private fun isPowerOfTen(value: Int): Boolean {
        var valueCalculation = value
        while (valueCalculation > 9 && valueCalculation % 10 == 0) {
            valueCalculation /= 10
        }
        return valueCalculation == 1
    }

    companion object {
        fun subUnitsOf(amountInSubunits: Long, tokenClass: Asset): Amount {
            return Amount(tokenClass, amountInSubunits)
        }

        fun of(amount: Long, tokenClass: Asset): Amount {
            return Amount(tokenClass, tokenClass.subUnits * amount)
        }

        fun of(amount: BigDecimal, tokenClass: Asset): Amount {
            val subUnitAmount = amount.multiply(BigDecimal.valueOf(tokenClass.subUnits.toLong())).stripTrailingZeros()
            if (subUnitAmount.scale() > 0) {
                throw IllegalArgumentException(
                    "Amount $amount cannot be used for $tokenClass which has a subunit value of ${tokenClass.subUnits}"
                )
            }

            return Amount(tokenClass, subUnitAmount.longValueExact())
        }
    }
}
