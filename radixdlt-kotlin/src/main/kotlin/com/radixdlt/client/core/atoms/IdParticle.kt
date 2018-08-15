package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey

class IdParticle(private val applicationId: String, private val uniqueId: EUID, destinations: Set<EUID>, owners: Set<ECKeyPair>) : Particle(destinations, owners) {
    companion object {
        @JvmStatic
        fun create(applicationId: String, uniqueId: EUID, key: ECPublicKey): IdParticle {
            return IdParticle(applicationId, uniqueId, setOf(key.getUID()), setOf(key.toECKeyPair()))
        }
    }
}
