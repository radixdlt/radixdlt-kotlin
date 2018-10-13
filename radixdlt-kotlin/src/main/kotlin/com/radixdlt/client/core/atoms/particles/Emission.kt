package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AccountReference

class Emission(quantity: Long, addresses: List<AccountReference>, nonce: Long, assetId: EUID, planck: Long) :
    Consumable(quantity, addresses, nonce, assetId, planck,
        Spin.UP
    )
