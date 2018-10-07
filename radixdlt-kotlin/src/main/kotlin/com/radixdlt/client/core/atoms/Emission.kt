package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID

class Emission(quantity: Long, addresses: List<AccountReference>, nonce: Long, assetId: EUID, planck: Long) :
    Consumable(quantity, addresses, nonce, assetId, planck, Spin.UP)
