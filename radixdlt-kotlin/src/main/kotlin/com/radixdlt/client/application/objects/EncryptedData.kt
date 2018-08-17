package com.radixdlt.client.application.objects

import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.crypto.ECKeyPairGenerator
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import java.util.ArrayList
import java.util.HashMap

/**
 * Application layer encrypted data object. Can be stored and retrieved from a RadixAddress.
 */
class EncryptedData private constructor(val encrypted: ByteArray, val metaData: Map<String, Any>, val protectors: List<EncryptedPrivateKey>) {

    class EncryptedDataBuilder {
        private val metaData = HashMap<String, Any>()
        private var data: ByteArray? = null
        private val readers = ArrayList<ECPublicKey>()

        fun metaData(key: String, value: Any): EncryptedDataBuilder {
            metaData[key] = value
            return this
        }

        fun data(data: ByteArray): EncryptedDataBuilder {
            this.data = data
            return this
        }

        fun addReader(reader: ECPublicKey): EncryptedDataBuilder {
            readers.add(reader)
            return this
        }

        fun build(): EncryptedData {
            val sharedKey = ECKeyPairGenerator.newInstance().generateKeyPair()
            val protectors = readers.asSequence().map { sharedKey.encryptPrivateKey(it) }.toList()
            val encrypted = sharedKey.getPublicKey().encrypt(data!!)

            return EncryptedData(encrypted, metaData, protectors)
        }
    }

    companion object {

        fun fromAtom(atom: ApplicationPayloadAtom): EncryptedData {
            val metaData = HashMap<String, Any>()
            metaData["timestamp"] = atom.timestamp
            metaData["signatures"] = atom.signatures!!
            metaData["application"] = atom.applicationId

            val protectors = atom.encryptor!!.protectors

            return EncryptedData(
                    atom.encrypted!!.bytes,
                    metaData,
                    protectors
            )
        }
    }
}
