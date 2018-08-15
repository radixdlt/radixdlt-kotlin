package com.radixdlt.client.wallet

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.assets.AssetAmount

class InsufficientFundsException(private val asset: Asset, val available: Long, val requestedAmount: Long) : Exception("Requested " + AssetAmount(asset, requestedAmount)
        + " but only " + AssetAmount(asset, available) + " available.") {

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
