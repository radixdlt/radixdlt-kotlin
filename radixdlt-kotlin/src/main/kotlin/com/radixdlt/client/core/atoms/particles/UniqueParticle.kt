package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.Objects

class UniqueParticle(
    // TODO: make immutable
    private val unique: Payload,
    private val destinations: Set<EUID>,
    private val owners: Set<ECKeyPair>
) : Particle {

    private val spin = Spin.UP

    init {
        Objects.requireNonNull(unique)
    }

    override fun getSpin(): Spin {
        return spin
    }

    override fun getAddresses(): Set<ECPublicKey> {
        return owners.asSequence().map(ECKeyPair::getPublicKey).toSet()
    }

    companion object {
        @JvmStatic
        fun create(unique: Payload, key: ECPublicKey): UniqueParticle {
            return UniqueParticle(
                unique,
                setOf(key.getUID()),
                setOf(key.toECKeyPair())
            )
        }
    }
}
