package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.crypto.Encryptor
import java.util.ArrayList
import java.util.HashSet
import java.util.Objects

class AtomBuilder {

    private val destinations = HashSet<EUID>()
    private val particles = ArrayList<Particle>()
    private val signature: ECSignature? = null
    private val signatureId: EUID? = null
    private var timestamp: Long? = null
    private var sharedKey: ECKeyPair? = null
    private var applicationId: String? = null
    private var payloadRaw: ByteArray? = null
    private var atomClass: Class<out Atom>? = null
    private var encryptor: Encryptor? = null
    private var payload: Payload? = null

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

    fun <T : Atom> type(atomClass: Class<T>): AtomBuilder {
        this.atomClass = atomClass
        return this
    }

    fun buildWithPOWFee(magic: Int, owner: ECPublicKey): UnsignedAtom {
        // Expensive but fine for now
        val unsignedAtom = this.build()
        val size = unsignedAtom.rawAtom.toDson().size

        val fee = AtomFeeConsumableBuilder()
            .atom(unsignedAtom)
            .owner(owner)
            .pow(magic, Math.ceil(Math.log(size * 8.0)).toInt())
            .build()

        this.addParticle(fee)

        return this.build()
    }

    fun build(): UnsignedAtom {
        Objects.requireNonNull<Class<out Atom>>(atomClass)

        if (this.timestamp == null) {
            this.timestamp = System.currentTimeMillis()
        }

        if (this.payloadRaw != null) {
            this.payload = Payload(this.payloadRaw!!)
        }

        // TODO: add this check to when payloadRaw is first set
        if (payload != null && payload!!.length() > MAX_PAYLOAD_SIZE) {
            throw IllegalStateException("Payload must be under " + MAX_PAYLOAD_SIZE + " bytes but was " + payload!!.length())
        }

        val atom: Atom
        if (PayloadAtom::class.java.isAssignableFrom(atomClass!!)) {
            atom = PayloadAtom(applicationId, particles, destinations, payload, encryptor, this.timestamp!!)
        } else {
            throw IllegalStateException("Unable to create atom with class: " + atomClass!!.simpleName)
        }

        return UnsignedAtom(atom)
    }

    companion object {
        private const val MAX_PAYLOAD_SIZE = 1028
    }
}
