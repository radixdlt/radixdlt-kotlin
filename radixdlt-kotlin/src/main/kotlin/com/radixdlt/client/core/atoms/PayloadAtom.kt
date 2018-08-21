package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.Encryptor

abstract class PayloadAtom : Atom {
    val encrypted: Payload?
    val encryptor: Encryptor?

    internal constructor(
        destinations: Set<EUID>,
        encrypted: Payload?,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) : super(destinations, timestamp, signatureId, signature) {
        this.encrypted = encrypted
        this.encryptor = null
    }

    internal constructor(
        particles: List<Particle>,
        destinations: Set<EUID>,
        encrypted: Payload?,
        encryptor: Encryptor?,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) : super(particles, destinations, timestamp, signatureId, signature) {
        this.encrypted = encrypted
        this.encryptor = encryptor
    }

    internal constructor(
        particles: List<Particle>,
        destinations: Set<EUID>,
        encrypted: Payload?,
        timestamp: Long,
        signatureId: EUID,
        signature: ECSignature
    ) : super(particles, destinations, timestamp, signatureId, signature) {
        this.encrypted = encrypted
        this.encryptor = null
    }

    internal constructor(
        particles: List<Particle>,
        destinations: Set<EUID>,
        encrypted: Payload?,
        encryptor: Encryptor?,
        timestamp: Long
    ) : super(destinations, particles, timestamp) {
        this.encrypted = encrypted
        this.encryptor = encryptor
    }

    internal constructor(
        destinations: Set<EUID>,
        encrypted: Payload?,
        particles: List<Particle>,
        timestamp: Long
    ) : super(destinations, particles, timestamp) {
        this.encrypted = encrypted
        this.encryptor = null
    }
}
