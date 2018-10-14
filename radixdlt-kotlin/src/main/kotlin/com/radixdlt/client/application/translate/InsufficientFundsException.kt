package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.TokenRef
import java.math.BigDecimal

class InsufficientFundsException(
    private val tokenRef: TokenRef,
    val available: BigDecimal,
    val requestedAmount: BigDecimal
) : Exception("Requested $requestedAmount but only $available ${tokenRef.iso} available.") {

    override fun equals(other: Any?): Boolean {
        if (other !is InsufficientFundsException) {
            return false
        }

        return (this.tokenRef == other.tokenRef
            && this.available.compareTo(other.available) == 0
            && this.requestedAmount.compareTo(other.requestedAmount) == 0)
    }

    override fun hashCode(): Int {
        return this.message!!.hashCode()
    }
}
