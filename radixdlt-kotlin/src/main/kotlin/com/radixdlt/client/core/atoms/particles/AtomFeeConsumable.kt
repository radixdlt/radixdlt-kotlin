package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AccountReference

class AtomFeeConsumable(quantity: Long, address: AccountReference, nonce: Long, tokenReference: String, planck: Long) :
    Consumable(quantity, address, nonce, tokenReference, planck, Spin.UP) {
    private val service: EUID = EUID(1)
}
