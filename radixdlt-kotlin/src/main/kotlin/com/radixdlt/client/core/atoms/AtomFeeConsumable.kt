package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair

class AtomFeeConsumable(quantity: Long, owners: Set<ECKeyPair>, nonce: Long, assetId: EUID) : Consumable(quantity, owners, nonce, assetId)
