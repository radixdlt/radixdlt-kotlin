package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey

class AccountReference(key: ECPublicKey) {
    private val key: ECKeyPair = key.toECKeyPair()

    fun getKey(): ECPublicKey {
        return key.getPublicKey()
    }

    override fun toString(): String {
        return key.getPublicKey().toString()
    }
}
