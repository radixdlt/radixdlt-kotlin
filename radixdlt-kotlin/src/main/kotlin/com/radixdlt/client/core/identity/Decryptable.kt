package com.radixdlt.client.core.identity

import com.radixdlt.client.core.atoms.EncryptedPayload

interface Decryptable<T> {
    fun getEncrypted(): EncryptedPayload?
    fun deserialize(decrypted: ByteArray): T
}