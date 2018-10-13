package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.atoms.AccountReference

class Minted(quantity: Long, address: AccountReference, nonce: Long, tokenReference: String, planck: Long) :
    Consumable(quantity, address, nonce, tokenReference, planck, Spin.UP)
