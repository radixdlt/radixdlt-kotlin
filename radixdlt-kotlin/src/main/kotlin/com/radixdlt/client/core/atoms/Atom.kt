package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
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

    val action: String?

    /**
     * This explicit use will be removed in the future
     */
    val destinations: Set<EUID>

    /**
     * This will be moved into a Chrono Particle in the future
     */
    private val timestamps: Map<String, Long>

    /**
     * This will be moved into a Transfer Particle in the future
     */
    private val consumables: List<Particle>?

    val signatures: Map<String, ECSignature>?

    /**
     * These will be moved into a more general particle list in the future
     */
    val dataParticle: DataParticle?
    val encryptor: EncryptorParticle?
    val uniqueParticle: UniqueParticle?

    @Transient
    private var debug: MutableMap<String, Long>? = HashMap()

    val shards: Set<Long>
        get() = destinations.asSequence().map(EUID::shard).toSet()

    val abstractConsumables: List<Particle>
        get() = if (consumables == null) emptyList() else Collections.unmodifiableList(consumables)

    // HACK
    val requiredFirstShard: Set<Long>
        get() = if (this.consumables != null && this.consumables.asSequence().any(Particle::isConsumer)) {
            consumables.asSequence()
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

    fun getConsumables(): List<Consumable> {
        return abstractConsumables.asSequence()
            .filter(Particle::isConsumable)
            .map(Particle::asConsumable)
            .toList()
    }

    fun getConsumers(): List<Consumer> {
        return abstractConsumables.asSequence()
            .filter(Particle::isConsumer)
            .map(Particle::asConsumer)
            .toList()
    }

    constructor(
        dataParticle: DataParticle?,
        consumables: List<Particle>,
        destinations: Set<EUID>,
        encryptor: EncryptorParticle?,
        uniqueParticle: UniqueParticle?,
        timestamp: Long
    ) {
        this.dataParticle = dataParticle
        this.consumables = consumables
        this.destinations = destinations
        this.encryptor = encryptor
        this.uniqueParticle = uniqueParticle
        this.timestamps = Collections.singletonMap("default", timestamp)
        this.signatures = null
        this.action = "STORE"
    }

    constructor(
        dataParticle: DataParticle?,
        consumables: List<Particle>,
        destinations: Set<EUID>,
        encryptor: EncryptorParticle?,
        uniqueParticle: UniqueParticle?,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) {
        this.dataParticle = dataParticle
        this.consumables = consumables
        this.destinations = destinations
        this.encryptor = encryptor
        this.uniqueParticle = uniqueParticle
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
        return abstractConsumables.asSequence()
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
        return abstractConsumables.asSequence()
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
        return "Atom hid($hid) destinations($destinations) consumables(${if (consumables == null) 0 else consumables.size})"
    }
}
