package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECSignature

class UnsignedAtom(val rawAtom: Atom) {

    val hash: RadixHash
        get() = rawAtom.hash

    fun sign(signature: ECSignature, signatureId: EUID): Atom {
        // TODO: Remove need to create a new object
        when (rawAtom) {
            is PayloadAtom -> {
                val unsigned = rawAtom
                return PayloadAtom(
                    unsigned.applicationId,
                    unsigned.particles!!,
                    unsigned.destinations,
                    unsigned.payload,
                    unsigned.encryptor!!,
                    rawAtom.timestamp,
                    signatureId,
                    signature
                )
            }
            else -> throw IllegalStateException("Cannot create signed atom")
        }
    }
}
