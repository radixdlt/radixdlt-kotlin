package com.radixdlt.client.application.translate

import com.radixdlt.client.application.objects.Amount
import com.radixdlt.client.core.atoms.TokenReference

class InsufficientFundsException(
    private val token: TokenReference,
    val available: Long,
    val requestedAmount: Long
) : Exception(
    "Requested ${Amount.subUnitsOf(requestedAmount, token)} but only ${Amount.subUnitsOf(available, token)} available."
) {

    override fun equals(other: Any?): Boolean {
        if (other !is InsufficientFundsException) {
            return false
        }

        val o = other as InsufficientFundsException?
        return this.token == o!!.token && this.available == o.available && this.requestedAmount == o.requestedAmount
    }

    override fun hashCode(): Int {
        return this.message!!.hashCode()
    }
}
