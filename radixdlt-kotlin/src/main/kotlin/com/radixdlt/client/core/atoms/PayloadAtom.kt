package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.Encryptor

class PayloadAtom : Atom {
    val applicationId: String?
    val payload: Payload?
    val encryptor: Encryptor?

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
        applicationId: String,
        destinations: Set<EUID>,
        payload: Payload,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) : super(destinations, timestamp, signatureId, signature) {
        this.payload = payload
        this.encryptor = null
        this.applicationId = applicationId
    }

    constructor(
        applicationId: String?,
        particles: List<Particle>,
        destinations: Set<EUID>,
        payload: Payload?,
        encryptor: Encryptor,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) : super(particles, destinations, timestamp, signatureId, signature) {
        this.payload = payload
        this.encryptor = encryptor
        this.applicationId = applicationId
    }

    internal constructor(
        applicationId: String?,
        particles: List<Particle>,
        destinations: Set<EUID>,
        payload: Payload?,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) : super(particles, destinations, timestamp, signatureId, signature) {
        this.payload = payload
        this.encryptor = null
        this.applicationId = applicationId
    }

    constructor(
        applicationId: String?,
        particles: List<Particle>,
        destinations: Set<EUID>,
        payload: Payload?,
        encryptor: Encryptor?,
        timestamp: Long
    ) : super(destinations, particles, timestamp) {
        this.payload = payload
        this.encryptor = encryptor
        this.applicationId = applicationId
    }

    internal constructor(
        applicationId: String?,
        destinations: Set<EUID>,
        payload: Payload,
        particles: List<Particle>,
        timestamp: Long
    ) : super(destinations, particles, timestamp) {
        this.payload = payload
        this.encryptor = null
        this.applicationId = applicationId
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

}
