package com.radixdlt.client.application.objects

import java.math.BigDecimal

/**
 * Class mainly for formatting amounts in error messages and other English text.
 */
class Amount(val token: Token, val amountInSubunits: Long) {

    fun lte(amount: Amount): Boolean {
        if (amount.token != this.token) {
            throw IllegalArgumentException("Only amounts with the same token class can be compared.")
        }

        return this.amountInSubunits <= amount.amountInSubunits
    }

    fun gte(amount: Amount): Boolean {
        if (amount.token != this.token) {
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
            return other.amountInSubunits == this.amountInSubunits && other.token == this.token
        }

        return false
    }

    override fun toString(): String {
        return "${formattedAmount()} ${token.iso}"
    }

    private fun formattedAmount(): String {
        val remainder = amountInSubunits % token.subUnits

        if (remainder == 0L) {
            // Whole number
            return (amountInSubunits / token.subUnits).toString()
        }

        if (isPowerOfTen(token.subUnits)) {
            // Decimal format
            return BigDecimal.valueOf(amountInSubunits).divide(BigDecimal.valueOf(token.subUnits.toLong())).toString()
        }

        // Fraction format
        val quotient = amountInSubunits / token.subUnits
        val fraction = remainder.toString() + "/" + token.subUnits
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
        fun subUnitsOf(amountInSubunits: Long, tokenClass: Token): Amount {
            return Amount(tokenClass, amountInSubunits)
        }

        fun of(amount: Long, tokenClass: Token): Amount {
            return Amount(tokenClass, tokenClass.subUnits * amount)
        }

        fun of(amount: BigDecimal, token: Token): Amount {
            val subUnitAmount = amount.multiply(BigDecimal.valueOf(token.subUnits.toLong())).stripTrailingZeros()
            if (subUnitAmount.scale() > 0) {
                throw IllegalArgumentException(
                    "Amount $amount cannot be used for $token which has a subunit value of ${token.subUnits}"
                )
            }

            return Amount(token, subUnitAmount.longValueExact())
        }
    }
}
