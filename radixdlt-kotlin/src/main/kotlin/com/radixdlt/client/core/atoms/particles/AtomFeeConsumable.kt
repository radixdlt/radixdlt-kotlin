package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.TokenRef

class AtomFeeConsumable(quantity: Long, address: AccountReference, nonce: Long, tokenRef: TokenRef, planck: Long) :
    Consumable(quantity, address, nonce, tokenRef, planck, Spin.UP) {
    private val service: EUID = EUID(1)
}
