package com.radixdlt.client.application.objects

import com.radixdlt.client.core.atoms.TokenReference
import java.math.BigDecimal

/**
 * Class mainly for formatting amounts in error messages and other English text.
 */
class Amount(val tokenReference: TokenReference, val amountInSubunits: Long) {

    fun lte(amount: Amount): Boolean {
        if (amount.tokenReference != this.tokenReference) {
            throw IllegalArgumentException("Only amounts with the same token class can be compared.")
        }

        return this.amountInSubunits <= amount.amountInSubunits
    }

    fun gte(amount: Amount): Boolean {
        if (amount.tokenReference != this.tokenReference) {
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
            return other.amountInSubunits == this.amountInSubunits && other.tokenReference == this.tokenReference
        }

        return false
    }

    override fun toString(): String {
        return "${formattedAmount()} ${tokenReference.iso}"
    }

    private fun formattedAmount(): String {
        val remainder = amountInSubunits % TokenReference.SUB_UNITS

        return if (remainder == 0L) {
            // Whole number
            (amountInSubunits / TokenReference.SUB_UNITS).toString()
        } else {
            // Decimal format
            BigDecimal.valueOf(amountInSubunits).divide(BigDecimal.valueOf(TokenReference.SUB_UNITS.toLong())).toString()
        }
    }

    companion object {
        fun subUnitsOf(amountInSubunits: Long, tokenReferenceClass: TokenReference): Amount {
            return Amount(tokenReferenceClass, amountInSubunits)
        }

        fun of(amount: Long, tokenReference: TokenReference): Amount {
            return Amount(tokenReference, TokenReference.SUB_UNITS * amount)
        }

        fun of(amount: BigDecimal, tokenReference: TokenReference): Amount {
            val subUnitAmount = amount.multiply(BigDecimal.valueOf(TokenReference.SUB_UNITS.toLong())).stripTrailingZeros()
            if (subUnitAmount.scale() > 0) {
                throw IllegalArgumentException(
                    "Amount $amount cannot be used for $tokenReference which has a subunit value of ${TokenReference.SUB_UNITS}"
                )
            }

            return Amount(tokenReference, subUnitAmount.longValueExact())
        }
    }
}
