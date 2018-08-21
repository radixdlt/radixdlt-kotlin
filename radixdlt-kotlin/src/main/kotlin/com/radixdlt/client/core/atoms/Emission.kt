package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair

class Emission : Consumable {
    constructor(quantity: Long, owner: ECKeyPair, nonce: Long, assetId: EUID) : super(quantity, owner, nonce, assetId)

    constructor(quantity: Long, owners: Set<ECKeyPair>, nonce: Long, assetId: EUID) : super(
        quantity,
        owners,
        nonce,
        assetId
    )
}
