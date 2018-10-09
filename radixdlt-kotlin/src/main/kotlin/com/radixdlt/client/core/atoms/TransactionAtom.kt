package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.Encryptor

class TransactionAtom : PayloadAtom {
    private val operation = "TRANSFER"

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

    internal constructor(
        particles: List<Particle>,
        destinations: Set<EUID>,
        payload: Payload?,
        timestamp: Long
    ) : super(destinations, payload, particles, timestamp)

    internal constructor(
        particles: List<Particle>,
        destinations: Set<EUID>,
        payload: Payload?,
        encryptor: Encryptor?,
        timestamp: Long
    ) : super(particles, destinations, payload, encryptor, timestamp)

    internal constructor(particles: List<Particle>, destinations: Set<EUID>, timestamp: Long) : super(
        destinations,
        null,
        particles,
        timestamp
    )

    internal constructor(
        particles: List<Particle>,
        destinations: Set<EUID>,
        payload: Payload?,
        encryptor: Encryptor?,
        signatureId: EUID,
        signature: ECSignature,
        timestamp: Long
    ) : super(particles, destinations, payload, encryptor, timestamp, signatureId, signature)

    fun getAbstractConsumables(): List<AbstractConsumable> {
        return particles!!.asSequence()
            .filter { it.isAbstractConsumable }
            .map { it.asAbstractConsumable }
            .toList()
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

    override fun toString(): String {
        return "${super.toString()} ${consumableSummary()}"
    }
}
