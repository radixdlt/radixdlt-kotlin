package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.Encryptor

class EncryptedPayload {
    val payload: Payload
    private val encryptor: Encryptor?

    constructor(payload: Payload) {
        this.payload = payload
        this.encryptor = null
    }

    constructor(payload: Payload, encryptor: Encryptor?) {
        this.payload = payload
        this.encryptor = encryptor
    }

    @Throws(CryptoException::class)
    fun decrypt(ecKeyPair: ECKeyPair): ByteArray {
        return encryptor?.decrypt(payload.bytes, ecKeyPair) ?: payload.bytes
    }
}