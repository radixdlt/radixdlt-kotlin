package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID
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

    override fun getDestinations(): Set<EUID> {
        return emptySet()
    }

    val timestamp: Long
        get() = timestamps["default"]!!
}
