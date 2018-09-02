package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey

class UniqueParticle(private val unique: Payload, destinations: Set<EUID>, owners: Set<ECKeyPair>) :
    Particle(destinations, owners) {

    companion object {
        @JvmStatic
        fun create(unique: Payload, key: ECPublicKey): UniqueParticle {
            return UniqueParticle(unique, setOf(key.getUID()), setOf(key.toECKeyPair()))
        }
    }
}
