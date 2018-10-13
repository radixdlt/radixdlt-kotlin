package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.ArrayList

class AtomBuilder {

    private val particles = ArrayList<Particle>()

    fun addParticles(particles: List<Particle>): AtomBuilder {
        this.particles.addAll(particles)
        return this
    }

    fun addParticle(particle: Particle): AtomBuilder {
        this.particles.add(particle)
        return this
    }

    fun buildWithPOWFee(magic: Int, owner: ECPublicKey): UnsignedAtom {
        val timestamp = System.currentTimeMillis()

        // Expensive but fine for now
        val unsignedAtom = this.build(timestamp)

        // Rebuild with atom fee
        val fee = AtomFeeConsumableBuilder()
            .atom(unsignedAtom)
            .owner(owner)
            .pow(magic, POW_LEADING_ZEROES_REQUIRED)
            .build()
        this.addParticle(fee)

        return this.build(timestamp)
    }

    fun build(timestamp: Long): UnsignedAtom {
        val particles = ArrayList(this.particles)
        particles.add(ChronoParticle(timestamp))
        return UnsignedAtom(Atom(particles))
    }

    // Temporary method for testing
    fun build(): UnsignedAtom {
        return this.build(System.currentTimeMillis())
    }

    companion object {
        private const val POW_LEADING_ZEROES_REQUIRED = 16
    }
}
