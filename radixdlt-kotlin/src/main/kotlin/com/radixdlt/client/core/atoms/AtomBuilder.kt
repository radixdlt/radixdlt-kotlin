package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.crypto.Encryptor
import java.util.ArrayList
import java.util.HashSet

class AtomBuilder {

    private val destinations = HashSet<EUID>()
    private val particles = ArrayList<Particle>()
    private var applicationId: String? = null
    private var payloadRaw: ByteArray? = null
    private var encryptor: Encryptor? = null

    fun addDestination(euid: EUID): AtomBuilder {
        this.destinations.add(euid)
        return this
    }

    fun addDestination(address: RadixAddress): AtomBuilder {
        return this.addDestination(address.getUID())
    }

    fun applicationId(applicationId: String): AtomBuilder {
        this.applicationId = applicationId
        return this
    }

    fun payload(payloadRaw: ByteArray?): AtomBuilder {
        this.payloadRaw = payloadRaw
        return this
    }

    fun payload(payloadRaw: String): AtomBuilder {
        return this.payload(payloadRaw.toByteArray())
    }

    fun protectors(protectors: List<EncryptedPrivateKey>): AtomBuilder {
        this.encryptor = Encryptor(protectors)
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
        val payload: Payload? = if (this.payloadRaw != null) {
            if (payloadRaw!!.size > MAX_PAYLOAD_SIZE) {
                throw IllegalStateException("Payload must be under $MAX_PAYLOAD_SIZE bytes but was ${payloadRaw!!.size}")
            }
            Payload(this.payloadRaw!!)
        } else {
            null
        }

        return UnsignedAtom(Atom(applicationId, particles, destinations, payload, encryptor, timestamp))
    }

    // Temporary method for testing
    fun build(): UnsignedAtom {
        return this.build(System.currentTimeMillis())
    }

    companion object {
        private const val MAX_PAYLOAD_SIZE = 1028
    }
}
