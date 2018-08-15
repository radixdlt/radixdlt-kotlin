package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.Encryptor


class ApplicationPayloadAtom : PayloadAtom {
    val applicationId: String

    constructor(applicationId: String,
            particles: List<Particle>,
            destinations: Set<EUID>,
            encrypted: Payload?,
            encryptor: Encryptor?,
            timestamp: Long
    ) : super(particles, destinations, encrypted, encryptor, timestamp) {
        this.applicationId = applicationId
    }


    constructor(
            applicationId: String,
            particles: List<Particle>,
            destinations: Set<EUID>,
            encrypted: Payload?,
            encryptor: Encryptor?,
            timestamp: Long,
            signatureId: EUID,
            signature: ECSignature
    ) : super(particles, destinations, encrypted, encryptor, timestamp, signatureId, signature) {
        this.applicationId = applicationId
    }
}
