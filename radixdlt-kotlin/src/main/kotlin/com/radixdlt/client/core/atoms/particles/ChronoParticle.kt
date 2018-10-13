package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.Collections

/**
 * Particle which stores time related aspects of an atom.
 */
class ChronoParticle(timestamp: Long) : Particle {

    private val spin = Spin.UP
    private val timestamps: Map<String, Long> = Collections.singletonMap("default", timestamp)

    override fun getSpin(): Spin {
        return spin
    }

    override fun getAddresses(): Set<ECPublicKey> {
        return emptySet()
    }

    val timestamp: Long
        get() = timestamps["default"]!!
}
