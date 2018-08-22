package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.serialization.Dson
import java.util.Collections
import java.util.HashMap

abstract class Atom {
    val destinations: Set<EUID>
    private val timestamps: Map<String, Long>?

    val timestamp: Long
        get() = timestamps?.get("default")!!

    val action: String

    val particles: List<Particle>?
        get() = if (field == null) emptyList() else Collections.unmodifiableList(field)

    val signatures: Map<String, ECSignature>?

    @Transient
    private var debug: MutableMap<String, Long>? = HashMap()

    val shards: Set<Long>
        get() = destinations.asSequence().map(EUID::shard).toSet()

    val asTransactionAtom: TransactionAtom
        get() = this as TransactionAtom

    val hash: RadixHash
        get() = RadixHash.of(Dson.instance.toDson(this))

    val hid: EUID
        get() = hash.toEUID()

    constructor() {
        this.destinations = emptySet()
        this.timestamps = null
        this.particles = null
        this.signatures = null
        this.action = "STORE"
    }

    constructor(destinations: Set<EUID>, timestamp: Long, signatureId: EUID?, signature: ECSignature) {
        this.destinations = destinations
        this.particles = null
        this.timestamps = Collections.singletonMap("default", timestamp)
        this.action = "STORE"
        // HACK
        // TODO: fix this
        this.signatures = if (signatureId == null) null else Collections.singletonMap(signatureId.toString(), signature)
    }

    constructor(destinations: Set<EUID>, particles: List<Particle>, timestamp: Long) {
        this.destinations = destinations
        this.particles = particles
        this.timestamps = Collections.singletonMap("default", timestamp)
        this.signatures = null
        this.action = "STORE"
    }

    constructor(
        particles: List<Particle>,
        destinations: Set<EUID>,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) {
        this.destinations = destinations
        this.particles = particles
        this.timestamps = Collections.singletonMap("default", timestamp)
        this.signatures = Collections.singletonMap(signatureId.toString(), signature)
        this.action = "STORE"
    }

    // HACK
    val requiredFirstShard: Set<Long>
        get() = if (this.particles != null && this.particles!!.asSequence().any(Particle::isConsumer)) {
            particles!!.asSequence()
                .filter(Particle::isConsumer)
                .flatMap { particle -> particle.destinations!!.asSequence() }
                .map(EUID::shard)
                .toSet()
        } else {
            shards
        }

    fun getSignature(uid: EUID): ECSignature? {
        return signatures?.get(uid.toString())
    }

    fun toDson(): ByteArray {
        return Dson.instance.toDson(this)
    }

    fun getDebug(name: String): Long? {
        if (debug == null) {
            debug = HashMap()
        }
        return debug?.get(name)
    }

    fun putDebug(name: String, value: Long) {
        if (debug == null) {
            debug = HashMap()
        }
        debug!![name] = value
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

    override fun toString(): String {
        return "Atom hid($hid) destinations($destinations)"
    }
}
