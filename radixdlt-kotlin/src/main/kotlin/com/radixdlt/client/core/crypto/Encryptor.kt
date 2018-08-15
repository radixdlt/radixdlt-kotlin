package com.radixdlt.client.core.crypto

import java.util.*

class Encryptor(protectors: List<EncryptedPrivateKey>) {

    private val protectors: List<EncryptedPrivateKey> = ArrayList(protectors)

    @Throws(CryptoException::class)
    fun decrypt(data: ByteArray, accessor: ECKeyPair): ByteArray {
        for (protector in protectors) {
            // TODO: remove exception catching
            try {
                return accessor.decrypt(data, protector)
            } catch (e: MacMismatchException) {
            }

        }

        throw CryptoException("Unable to decrypt any of the " + protectors.size + " protectors.")
    }
}
