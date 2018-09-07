package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECSignature

class UnsignedAtom(val rawAtom: Atom) {

    val hash: RadixHash
        get() = rawAtom.hash

    fun sign(signature: ECSignature, signatureId: EUID): Atom {
        // TODO: Remove need to create a new object
        val atom = rawAtom
        return Atom(
            atom.dataParticle,
            atom.abstractConsumables,
            atom.destinations,
            atom.encryptor,
            atom.uniqueParticle,
            rawAtom.timestamp!!,
            signatureId,
            signature
        )
    }
}
