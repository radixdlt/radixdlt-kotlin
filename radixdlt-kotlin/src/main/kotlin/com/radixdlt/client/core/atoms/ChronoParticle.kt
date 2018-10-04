package com.radixdlt.client.core.atoms

import java.util.Collections

/**
 * Particle which stores time related aspects of an atom.
 */
class ChronoParticle(timestamp: Long) : Particle(1) {
    private val timestamps: Map<String, Long> = Collections.singletonMap("default", timestamp)

    val timestamp: Long
        get() = timestamps["default"]!!
}
