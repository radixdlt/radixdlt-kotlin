package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.TokenReference
import java.math.BigDecimal

class InsufficientFundsException(
    private val tokenReference: TokenReference,
    val available: BigDecimal,
    val requestedAmount: BigDecimal
) : Exception("Requested $requestedAmount but only $available ${tokenReference.iso} available.") {

    override fun equals(other: Any?): Boolean {
        if (other !is InsufficientFundsException) {
            return false
        }

        return (this.tokenReference == other.tokenReference
            && this.available.compareTo(other.available) == 0
            && this.requestedAmount.compareTo(other.requestedAmount) == 0)
    }

    override fun hashCode(): Int {
        return this.message!!.hashCode()
    }
}
