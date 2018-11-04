package com.radixdlt.client.core.crypto

import java.util.ArrayList

class Encryptor(protectors: List<EncryptedPrivateKey>) {

    val protectors: List<EncryptedPrivateKey> = ArrayList(protectors)

    @Throws(CryptoException::class)
    fun decrypt(data: ByteArray, accessor: ECKeyPair): ByteArray {
        for (protector in protectors) {
            // TODO: remove exception catching
            try {
                return accessor.decrypt(data, protector)
            } catch (e: CryptoException) {
            }
        }

        throw CryptoException("Unable to decrypt any of the " + protectors.size + " protectors.")
    }
}
