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
        val remainder = amountInSubunits % Token.SUB_UNITS

        return if (remainder == 0L) {
            // Whole number
            (amountInSubunits / Token.SUB_UNITS).toString()
        } else {
            // Decimal format
            BigDecimal.valueOf(amountInSubunits).divide(BigDecimal.valueOf(Token.SUB_UNITS.toLong())).toString()
        }
    }

    companion object {
        fun subUnitsOf(amountInSubunits: Long, tokenClass: Token): Amount {
            return Amount(tokenClass, amountInSubunits)
        }

        fun of(amount: Long, token: Token): Amount {
            return Amount(token, Token.SUB_UNITS * amount)
        }

        fun of(amount: BigDecimal, token: Token): Amount {
            val subUnitAmount = amount.multiply(BigDecimal.valueOf(Token.SUB_UNITS.toLong())).stripTrailingZeros()
            if (subUnitAmount.scale() > 0) {
                throw IllegalArgumentException(
                    "Amount $amount cannot be used for $token which has a subunit value of ${Token.SUB_UNITS}"
                )
            }

            return Amount(token, subUnitAmount.longValueExact())
        }
    }
}
