package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECSignature

class UnsignedAtom(val rawAtom: Atom) {

    val hash: RadixHash
        get() = rawAtom.hash

    fun sign(signature: ECSignature, signatureId: EUID): Atom {
        // TODO: Remove need to create a new object
        when (rawAtom) {
            is TransactionAtom -> {
                val unsigned = rawAtom
                return TransactionAtom(
                    unsigned.applicationId,
                    unsigned.particles!!,
                    unsigned.destinations,
                    unsigned.encrypted,
                    unsigned.encryptor,
                    signatureId,
                    signature,
                    rawAtom.timestamp
                )
            }
            else -> throw IllegalStateException("Cannot create signed atom")
        }
    }
}
