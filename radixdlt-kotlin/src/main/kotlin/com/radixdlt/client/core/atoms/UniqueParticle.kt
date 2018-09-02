package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey

class UniqueParticle(
    // TODO: make immutable
    private val unique: Payload,
    private val destinations: Set<EUID>,
    private val owners: Set<ECKeyPair>
) {

    companion object {
        @JvmStatic
        fun create(unique: Payload, key: ECPublicKey): UniqueParticle {
            return UniqueParticle(unique, setOf(key.getUID()), setOf(key.toECKeyPair()))
        }
    }
}
