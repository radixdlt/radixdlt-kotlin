package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.ArrayList

class AtomBuilder {

    private val consumables = ArrayList<AbstractConsumable>()
    private var dataParticles = ArrayList<DataParticle>()
    private var uniqueParticle: UniqueParticle? = null

    fun setUniqueParticle(uniqueParticle: UniqueParticle): AtomBuilder {
        this.uniqueParticle = uniqueParticle
        return this
    }

    fun addDataParticle(dataParticle: DataParticle): AtomBuilder {
        this.dataParticles.add(dataParticle)
        return this
    }

    fun addConsumable(consumable: Consumable): AtomBuilder {
        this.consumables.add(consumable)
        return this
    }

    fun <T : Consumable> addConsumables(particles: List<T>): AtomBuilder {
        this.consumables.addAll(particles)
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
        this.addConsumable(fee)

        return this.build(timestamp)
    }

    fun build(timestamp: Long): UnsignedAtom {
        val particles = ArrayList<Particle>()
        particles.addAll(dataParticles)
        particles.addAll(consumables)
        if (uniqueParticle != null) {
            particles.add(uniqueParticle!!)
        }
        particles.add(ChronoParticle(timestamp))
        return UnsignedAtom(Atom(particles))
    }

    // Temporary method for testing
    fun build(): UnsignedAtom {
        return this.build(System.currentTimeMillis())
    }

    companion object {
        private val POW_LEADING_ZEROES_REQUIRED = 16
    }
}
