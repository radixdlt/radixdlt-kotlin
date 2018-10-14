package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.Token

class AtomFeeConsumable(quantity: Long, address: AccountReference, nonce: Long, tokenReference: Token, planck: Long) :
    Consumable(quantity, address, nonce, tokenReference, planck, Spin.UP) {
    private val service: EUID = EUID(1)
}
