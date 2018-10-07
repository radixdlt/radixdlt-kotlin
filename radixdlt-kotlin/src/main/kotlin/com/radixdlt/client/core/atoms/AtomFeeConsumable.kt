package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID

class AtomFeeConsumable(quantity: Long, addresses: List<AccountReference>, nonce: Long, assetId: EUID, planck: Long) :
    Consumable(quantity, addresses, nonce, assetId, planck, 1L) {
    private val service: EUID = EUID(1)
}

