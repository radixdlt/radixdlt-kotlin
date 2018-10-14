package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.particles.ChronoParticle
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.DataParticle
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.serialization.Dson
import java.util.Collections
import java.util.HashMap

/**
 * An atom is the fundamental atomic unit of storage on the ledger (similar to a block
 * in a blockchain) and defines the actions that can be issued onto the ledger.
 */
class Atom {

    val particles: List<Particle>?
        get() = field ?: emptyList()

    val signatures: Map<String, ECSignature>?

    @Transient
    private var debug: MutableMap<String, Long>? = HashMap()

    val shards: Set<Long>
        get() = particles!!.asSequence()
            .map(Particle::getAddresses)
            .flatMap { it -> it.asSequence() }
            .map(ECPublicKey::getUID)
            .map(EUID::shard)
            .toSet()

    // HACK
    val requiredFirstShard: Set<Long>
        get() = if (this.particles!!.asSequence().any { p -> p.getSpin() == Spin.DOWN }) {
            particles!!.asSequence()
                .filter { p -> p.getSpin() == Spin.DOWN }
                .flatMap { it.getAddresses().asSequence() }
                .map(ECPublicKey::getUID)
                .map(EUID::shard)
                .toSet()
        } else {
            shards
        }

    fun particles(spin: Spin): List<Particle> {
        return particles!!.asSequence()
            .filter { p -> p.getSpin() === spin }
            .toList()
    }

    val addresses: List<ECPublicKey>
        get() = particles!!.asSequence()
            .map { it.getAddresses() }
            .flatMap { it.asSequence() }
            .toList()

    val timestamp: Long
        get() = this.particles!!.asSequence()
            .filter { p -> p is ChronoParticle }
            .map { p -> (p as ChronoParticle).timestamp }
            .firstOrNull() ?: 0L

    val hash: RadixHash
        get() = RadixHash.of(Dson.instance.toDson(this))

    val hid: EUID
        get() = hash.toEUID()

    constructor(particles: List<Particle>?) {
        this.particles = particles
        this.signatures = null
    }

    private constructor(particles: List<Particle>?, signatureId: EUID, signature: ECSignature) {
        this.particles = particles
        this.signatures = Collections.singletonMap(signatureId.toString(), signature)
    }

    fun consumables(): List<Consumable> {
        return this.particles!!.asSequence()
            .filter { p -> p is Consumable }
            .map { p -> p as Consumable }
            .toList()
    }

    fun getConsumables(): List<Consumable> {
        return this.particles!!.asSequence()
            .filter { p -> p is Consumable }
            .map { p -> p as Consumable }
            .toList()
    }

    fun getConsumables(spin: Spin): List<Consumable> {
        return this.particles!!.asSequence()
            .filter { p -> p is Consumable }
            .filter { p -> p.getSpin() == spin }
            .map { p -> p as Consumable }
            .toList()
    }

    fun getDataParticles(): List<DataParticle>? {
        return this.particles!!.asSequence()
            .filter { p -> p is DataParticle }
            .map { p -> p as DataParticle }
            .toList()
    }

    fun withSignature(signature: ECSignature, signatureId: EUID): Atom {
        return Atom(
            particles,
            signatureId,
            signature
        )
    }

    fun getSignature(uid: EUID): ECSignature? {
        return signatures?.get(uid.toString())
    }

    fun toDson(): ByteArray {
        return Dson.instance.toDson(this)
    }

    fun tokenSummary(): Map<TokenRef, Map<ECPublicKey, Long>> {
        return consumables()
            .groupBy(Consumable::tokenRef)
            .mapValues { it ->
                it.value.asSequence().groupBy(Consumable::getOwner) {
                    it.getSignedAmount()
                }.mapValues {
                    it.value.sum()
                }
            }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Atom) {
            return false
        }

        return hash == other.hash
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    fun getDebug(name: String): Long {
        if (debug == null) {
            debug = HashMap()
        }
        return debug!![name]!!
    }

    fun putDebug(name: String, value: Long) {
        if (debug == null) {
            debug = HashMap()
        }
        debug!![name] = value
    }

    override fun toString(): String {
        return "Atom particles($hid)"
    }
}
