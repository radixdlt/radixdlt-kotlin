package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair

open class Consumable : AbstractConsumable {

    override val signedQuantity: Long
        get() = quantity

    constructor(quantity: Long, owner: ECKeyPair, nonce: Long, assetId: EUID) : super(quantity, setOf<ECKeyPair>(owner), nonce, assetId)

    constructor(quantity: Long, owners: Set<ECKeyPair>, nonce: Long, assetId: EUID) : super(quantity, owners, nonce, assetId)

    fun toConsumer(): Consumer {
        return Consumer(quantity, owners!!, nonce, assetId)
    }
}