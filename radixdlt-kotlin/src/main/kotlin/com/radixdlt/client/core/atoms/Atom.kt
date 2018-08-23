package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.Encryptor
import com.radixdlt.client.core.serialization.Dson
import java.util.Collections
import java.util.HashMap

class Atom {

    val destinations: Set<EUID>

    private val timestamps: Map<String, Long>
    val action: String?

    val particles: List<Particle>?
        get() = if (field == null) emptyList() else Collections.unmodifiableList(field)

    val signatures: Map<String, ECSignature>?

    @Transient
    private var debug: MutableMap<String, Long>? = HashMap()

    val applicationId: String?
    val payload: Payload?
    val encryptor: Encryptor?

    val shards: Set<Long>
        get() = destinations.asSequence().map(EUID::shard).toSet()

    // HACK
    val requiredFirstShard: Set<Long>
        get() = if (this.particles != null && this.particles!!.asSequence().any(Particle::isConsumer)) {
            particles!!.asSequence()
                .filter(Particle::isConsumer)
                .flatMap { it.destinations!!.asSequence() }
                .map(EUID::shard)
                .toSet()
        } else {
            shards
        }

    val timestamp: Long?
        get() = timestamps["default"]

    val hash: RadixHash
        get() = RadixHash.of(Dson.instance.toDson(this))

    val hid: EUID
        get() = hash.toEUID()

    val consumables: List<Consumable>
        get() = particles!!.asSequence()
            .filter(Particle::isConsumable)
            .map(Particle::asConsumable)
            .toList()

    val consumers: List<Consumer>
        get() = particles!!.asSequence()
            .filter(Particle::isConsumer)
            .map(Particle::asConsumer)
            .toList()

    constructor(
        applicationId: String?,
        particles: List<Particle>,
        destinations: Set<EUID>,
        bytes: Payload?,
        encryptor: Encryptor?,
        timestamp: Long
    ) {
        this.applicationId = applicationId
        this.particles = particles
        this.destinations = destinations
        this.payload = bytes
        this.encryptor = encryptor
        this.timestamps = Collections.singletonMap("default", timestamp)
        this.signatures = null
        this.action = "STORE"
    }

    constructor(
        applicationId: String,
        particles: List<Particle>,
        destinations: Set<EUID>,
        bytes: Payload?,
        encryptor: Encryptor?,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) {
        this.applicationId = applicationId
        this.particles = particles
        this.destinations = destinations
        this.payload = bytes
        this.encryptor = encryptor
        this.timestamps = Collections.singletonMap("default", timestamp)
        this.signatures = Collections.singletonMap(signatureId.toString(), signature)
        this.action = "STORE"
    }

    fun getSignature(uid: EUID): ECSignature? {
        return signatures?.get(uid.toString())
    }

    fun toDson(): ByteArray {
        return Dson.instance.toDson(this)
    }

    fun summary(): Map<Set<ECPublicKey>, Map<EUID, Long>> {
        return particles!!.asSequence()
            .filter(Particle::isAbstractConsumable)
            .map(Particle::asAbstractConsumable)
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
        return particles!!.asSequence()
            .filter(Particle::isAbstractConsumable)
            .map(Particle::asAbstractConsumable)
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
        return "Atom hid($hid) destinations($destinations) particles(${if (particles == null) 0 else particles!!.size})"
    }
}
