package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.ArrayList
import java.util.HashSet

class AtomBuilder {

    private val destinations = HashSet<EUID>()
    private val particles = ArrayList<Particle>()
    private var encryptor: EncryptorParticle? = null
    private var dataParticle: DataParticle? = null

    fun addDestination(euid: EUID): AtomBuilder {
        this.destinations.add(euid)
        return this
    }

    fun addDestination(address: RadixAddress): AtomBuilder {
        return this.addDestination(address.getUID())
    }

    fun setDataParticle(dataParticle: DataParticle): AtomBuilder {
        this.dataParticle = dataParticle
        return this
    }

    fun setEncryptorParticle(encryptor: EncryptorParticle): AtomBuilder {
        this.encryptor = encryptor
        return this
    }

    fun addParticle(particle: Particle): AtomBuilder {
        this.particles.add(particle)
        this.destinations.addAll(particle.destinations!!)
        return this
    }

    fun <T : Particle> addParticles(particles: List<T>): AtomBuilder {
        this.particles.addAll(particles)
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
        this.addParticle(fee)

        return this.build(timestamp)
    }

    fun build(timestamp: Long): UnsignedAtom {
        return UnsignedAtom(Atom(dataParticle, particles, destinations, encryptor, timestamp))
    }

    // Temporary method for testing
    fun build(): UnsignedAtom {
        return this.build(System.currentTimeMillis())
    }
}
