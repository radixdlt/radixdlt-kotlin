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
    // TODO: Remove as action should be outside of Atom structure
    val action: String?

    // TODO: Remove when particles define destinations
    val destinations: Set<EUID>

    // TODO: These will be turned into a list of DeleteParticles in the future
    val consumers: List<Consumer>?
        get() = if (field == null) emptyList() else Collections.unmodifiableList(field)

    // TODO: These will be turned into a list of CreateParticles in the future
    val consumables: List<AbstractConsumable>?
        get() = if (field == null) emptyList() else Collections.unmodifiableList(field)

    val dataParticles: List<DataParticle>?
        get() = if (field == null) emptyList() else Collections.unmodifiableList(field)

    val uniqueParticle: UniqueParticle?
    val chronoParticle: ChronoParticle
    private val asset: AssetParticle?

    val signatures: Map<String, ECSignature>?

    @Transient
    private var debug: MutableMap<String, Long>? = HashMap()

    val shards: Set<Long>
        get() = destinations.asSequence().map(EUID::shard).toSet()

    // HACK
    val requiredFirstShard: Set<Long>
        get() = if (this.consumables != null && this.consumers!!.isNotEmpty()) {
            consumers!!.asSequence()
                .flatMap { it.destinations!!.asSequence() }
                .map(EUID::shard)
                .toSet()
        } else {
            shards
        }

    val timestamp: Long
        get() = chronoParticle.timestamp

    val hash: RadixHash
        get() = RadixHash.of(Dson.instance.toDson(this))

    val hid: EUID
        get() = hash.toEUID()

    constructor(
        dataParticles: List<DataParticle>?,
        consumers: List<Consumer>?,
        consumables: List<AbstractConsumable>?,
        destinations: Set<EUID>,
        uniqueParticle: UniqueParticle?,
        asset: AssetParticle?,
        timestamp: Long
    ) {
        this.dataParticles = dataParticles
        this.chronoParticle = ChronoParticle(timestamp)
        this.consumers = consumers
        this.consumables = consumables
        this.destinations = destinations
        this.uniqueParticle = uniqueParticle
        this.asset = asset
        this.signatures = null
        this.action = "STORE"
    }

    private constructor(
        dataParticles: List<DataParticle>?,
        consumers: List<Consumer>?,
        consumables: List<AbstractConsumable>?,
        destinations: Set<EUID>,
        uniqueParticle: UniqueParticle?,
        asset: AssetParticle?,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) {
        this.dataParticles = dataParticles
        this.consumers = consumers
        this.consumables = consumables
        this.destinations = destinations
        this.uniqueParticle = uniqueParticle
        this.asset = asset
        this.chronoParticle = ChronoParticle(timestamp)
        this.signatures = Collections.singletonMap(signatureId.toString(), signature)
        this.action = "STORE"
    }

    fun withSignature(signature: ECSignature, signatureId: EUID): Atom {
        return Atom(
            dataParticles,
            consumers,
            consumables,
            destinations,
            uniqueParticle,
            asset,
            timestamp,
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
        return consumers!!.asSequence().plus(consumables!!.asSequence())
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
        return consumers!!.asSequence().plus(consumables!!.asSequence())
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
        return ("Atom hid($hid) destinations($destinations) consumables(${consumables?.size}) " +
            "consumers(${consumers?.size})")
    }
}
