package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.ArrayList
import java.util.HashSet

class AtomBuilder {

    private val destinations = HashSet<EUID>()
    private val consumables = ArrayList<AbstractConsumable>()
    private val consumers = ArrayList<Consumer>()
    private var dataParticles = ArrayList<DataParticle>()
    private var uniqueParticle: UniqueParticle? = null

    fun addDestination(euid: EUID): AtomBuilder {
        this.destinations.add(euid)
        return this
    }

    fun addDestination(address: RadixAddress): AtomBuilder {
        return this.addDestination(address.getUID())
    }

    fun setUniqueParticle(uniqueParticle: UniqueParticle): AtomBuilder {
        this.uniqueParticle = uniqueParticle
        return this
    }

    fun addDataParticle(dataParticle: DataParticle): AtomBuilder {
        this.dataParticles.add(dataParticle)
        return this
    }

    fun addConsumer(consumer: Consumer): AtomBuilder {
        this.consumers.add(consumer)
        this.destinations.addAll(consumer.destinations!!)
        return this
    }

    fun addConsumable(consumable: Consumable): AtomBuilder {
        this.consumables.add(consumable)
        this.destinations.addAll(consumable.destinations!!)
        return this
    }

    fun <T : Consumable> addConsumables(particles: List<T>): AtomBuilder {
        this.consumables.addAll(particles)
        particles.asSequence().flatMap { particle -> particle.destinations!!.asSequence() }
            .forEach { destinations.add(it) }
        return this
    }

    fun buildWithPOWFee(magic: Int, owner: ECPublicKey): UnsignedAtom {
        val timestamp = System.currentTimeMillis()

        // Expensive but fine for now
        val unsignedAtom = this.build(timestamp)

        // Rebuild with atom fee
        val size = unsignedAtom.rawAtom.toDson().size
        val fee = AtomFeeConsumableBuilder()
            .atom(unsignedAtom)
            .owner(owner)
            .pow(magic, Math.ceil(Math.log(size * 8.0)).toInt())
            .build()
        this.addConsumable(fee)

        return this.build(timestamp)
    }

    fun build(timestamp: Long): UnsignedAtom {
        return UnsignedAtom(
            Atom(
                if (dataParticles.isEmpty()) null else dataParticles,
                if (consumers.isEmpty()) null else consumers, // Pretty nasty hack here. Need to fix.
                consumables,
                destinations,
                uniqueParticle,
                null,
                timestamp
            )
        )
    }

    // Temporary method for testing
    fun build(): UnsignedAtom {
        return this.build(System.currentTimeMillis())
    }
}
