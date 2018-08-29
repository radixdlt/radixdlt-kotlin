package com.radixdlt.client.core.crypto

import java.util.ArrayList
import java.util.Collections

class Encryptor(protectors: List<EncryptedPrivateKey>) {

    val protectors: List<EncryptedPrivateKey> = Collections.unmodifiableList(ArrayList(protectors))

    class EncryptorBuilder {
        private val readers = ArrayList<ECPublicKey>()
        private var sharedKey: ECKeyPair? = null

        val numReaders: Int
            get() = readers.size

        fun sharedKey(sharedKey: ECKeyPair): EncryptorBuilder {
            this.sharedKey = sharedKey
            return this
        }

        fun addReader(reader: ECPublicKey): EncryptorBuilder {
            readers.add(reader)
            return this
        }

        fun build(): Encryptor {
            val protectors = readers.asSequence()
                .map { sharedKey!!.encryptPrivateKey(it) }
                .toList()
            return Encryptor(protectors)
        }
    }

    @Throws(CryptoException::class)
    fun decrypt(data: ByteArray, accessor: ECKeyPair): ByteArray {
        for (protector in protectors) {
            // TODO: remove exception catching
            try {
                return accessor.decrypt(data, protector)
            } catch (e: MacMismatchException) {
            }
        }

        throw CryptoException("Unable to decrypt any of the ${protectors.size} protectors.")
    }
}
