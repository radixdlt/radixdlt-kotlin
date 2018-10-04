package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
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
    // TODO: These will be turned into a list of DeleteParticles in the future
    val consumers: List<Consumer>?
        get() = if (field == null) emptyList() else Collections.unmodifiableList(field)

    // TODO: These will be turned into a list of CreateParticles in the future
    val particles: List<Particle>?
        get() = field ?: emptyList()

    val signatures: Map<String, ECSignature>?

    @Transient
    private var debug: MutableMap<String, Long>? = HashMap()

    val shards: Set<Long>
        get() = consumers!!.asSequence()
            .map(Consumer::owners)
            .flatMap { it: Set<ECKeyPair>? -> it!!.asSequence()}
            .map(ECKeyPair::getUID)
            .map(EUID::shard).toSet()

    // HACK
    val requiredFirstShard: Set<Long>
        get() = if (this.consumers != null && this.consumers!!.isNotEmpty()) {
            consumers!!.asSequence()
                .flatMap { it.destinations.asSequence() }
                .map(EUID::shard)
                .toSet()
        } else {
            shards
        }

    val timestamp: Long
        get() = this.particles!!.asSequence()
            .filter { p -> p is ChronoParticle }
            .map { p -> (p as ChronoParticle).timestamp }
            .firstOrNull() ?: 0L

    val hash: RadixHash
        get() = RadixHash.of(Dson.instance.toDson(this))

    val hid: EUID
        get() = hash.toEUID()

    constructor(
        particles: List<Particle>?,
        consumers: List<Consumer>?
    ) {
        this.particles = particles
        this.consumers = consumers
        this.signatures = null
    }

    private constructor(
        particles: List<Particle>?,
        consumers: List<Consumer>?,
        signatureId: EUID,
        signature: ECSignature
    ) {
        this.particles = particles
        this.consumers = consumers
        this.signatures = Collections.singletonMap(signatureId.toString(), signature)
    }

    fun getConsumables(): List<AbstractConsumable>? {
        return this.particles!!.asSequence()
            .filter { p -> p is AbstractConsumable }
            .map { p -> p as AbstractConsumable }
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
            consumers,
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

    fun summary(): Map<Set<ECPublicKey>, Map<EUID, Long>> {
        return consumers!!.asSequence().plus(getConsumables()!!.asSequence())
            .groupBy(AbstractConsumable::ownersPublicKeys)
            .mapValues { it ->
                it.value.asSequence().groupBy(AbstractConsumable::assetId) {
                    it.signedQuantity
                }.mapValues {
                    it.value.sum()
                }
            }
    }

    fun consumableSummary(): Map<Set<ECPublicKey>, Map<EUID, List<Long>>> {
        return consumers!!.asSequence().plus(getConsumables()!!.asSequence())
            .groupBy(AbstractConsumable::ownersPublicKeys)
            .mapValues { it: Map.Entry<Set<ECPublicKey>, List<AbstractConsumable>> ->
                it.value.asSequence().groupBy(AbstractConsumable::assetId) {
                    it.signedQuantity
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
        return "Atom hid($hid)"
    }
}
