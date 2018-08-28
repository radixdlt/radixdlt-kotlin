package com.radixdlt.client.application.translate

import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset

class InsufficientFundsException(
    private val asset: Asset,
    val available: Long,
    val requestedAmount: Long
) : Exception(
    "Requested ${Amount.subUnitsOf(requestedAmount, asset)} but only ${Amount.subUnitsOf(available, asset)} available."
) {

    override fun equals(other: Any?): Boolean {
        if (other !is InsufficientFundsException) {
            return false
        }

        val o = other as InsufficientFundsException?
        return this.asset == o!!.asset && this.available == o.available && this.requestedAmount == o.requestedAmount
    }

    override fun hashCode(): Int {
        return this.message!!.hashCode()
    }
}
