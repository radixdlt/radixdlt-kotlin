package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID

open class Consumable(quantity: Long, addresses: List<AccountReference>, nonce: Long, assetId: EUID, planck: Long) :
    AbstractConsumable(quantity, addresses, nonce, assetId, planck) {

    override val signedQuantity: Long
        get() = super.amount

    fun toConsumer(): Consumer {
        return Consumer(super.amount, super.addresses, nonce, tokenClass, planck)
    }
}
