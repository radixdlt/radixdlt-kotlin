package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.atoms.particles.ChronoParticle
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.ArrayList

class AtomBuilder {

    private val particles = ArrayList<Particle>()

    fun addParticle(particle: Particle): AtomBuilder {
        this.particles.add(particle)
        return this
    }

    fun buildWithPOWFee(magic: Int, owner: ECPublicKey, powToken: TokenRef): UnsignedAtom {
        val timestamp = System.currentTimeMillis()

        // Expensive but fine for now
        val unsignedAtom = this.build(timestamp)

        // Rebuild with atom fee
        val fee = AtomFeeConsumableBuilder()
            .powToken(powToken)
            .atom(unsignedAtom.rawAtom)
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

    companion object {
        private const val POW_LEADING_ZEROES_REQUIRED = 16
    }
}
