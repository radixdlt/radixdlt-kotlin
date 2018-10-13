package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AccountReference

class AtomFeeConsumable(quantity: Long, address: AccountReference, nonce: Long, assetId: EUID, planck: Long) :
    Consumable(quantity, address, nonce, assetId, planck,
        Spin.UP
    ) {
    private val service: EUID = EUID(1)
}

