package com.radixdlt.client.application.objects

import com.radixdlt.client.core.crypto.ECKeyPairGenerator
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.Encryptor
import org.bouncycastle.util.encoders.Base64
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Application layer bytes bytes object. Can be stored and retrieved from a RadixAddress.
 */
class Data private constructor(
    val bytes: ByteArray?,
    private val metaData: Map<String, Any?>,
    // TODO: make unmodifiable
    val encryptor: Encryptor?
) {

    class DataBuilder {
        private val metaData = LinkedHashMap<String, Any?>()
        private var bytes: ByteArray? = null
        private val encryptorBuilder = Encryptor.EncryptorBuilder()
        private var unencrypted: Boolean = false

        fun metaData(key: String, value: Any): DataBuilder {
            metaData[key] = value
            return this
        }

        fun bytes(bytes: ByteArray): DataBuilder {
            this.bytes = bytes
            return this
        }

        fun addReader(reader: ECPublicKey): DataBuilder {
            encryptorBuilder.addReader(reader)
            return this
        }

        fun unencrypted(): DataBuilder {
            this.unencrypted = true
            return this
        }

        fun build(): Data {
            val bytes: ByteArray?
            val encryptor: Encryptor?

            if (unencrypted) {
                encryptor = null
                bytes = this.bytes
            } else {
                if (encryptorBuilder.numReaders == 0) {
                    throw IllegalStateException("Must either be unencrypted or have at least one reader.")
                }

                val sharedKey = ECKeyPairGenerator.newInstance().generateKeyPair()
                encryptorBuilder.sharedKey(sharedKey)
                encryptor = encryptorBuilder.build()
                bytes = sharedKey.getPublicKey().encrypt(this.bytes!!)
                metaData["encrypted"] = unencrypted
            }

            return Data(bytes!!, metaData, encryptor)
        }
    }

    fun getMetaData(): Map<String, Any?> {
        return Collections.unmodifiableMap(metaData)
    }

    override fun toString(): String {
        val encrypted = metaData["encrypted"] as Boolean

        return if (encrypted) "encrypted: ${Base64.toBase64String(bytes)}" else "unencrypted: ${String(bytes!!)}"
    }

    companion object {

        // TODO: Cleanup this interface
        @JvmStatic
        fun raw(bytes: ByteArray?, metaData: Map<String, Any?>, encryptor: Encryptor?): Data {
            return Data(bytes, metaData, encryptor)
        }
    }
}
