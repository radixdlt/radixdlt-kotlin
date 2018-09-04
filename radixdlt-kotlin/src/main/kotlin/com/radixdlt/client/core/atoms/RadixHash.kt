package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.util.Hash
import com.radixdlt.client.core.util.Int128
import java.math.BigInteger
import java.nio.ByteBuffer
import org.bouncycastle.util.encoders.Base64

class RadixHash private constructor(private val hash: ByteArray) {

    fun toByteArray(): ByteArray {
        return hash.copyOf()
    }

    fun toEUID(): EUID {
        return EUID(Int128.from(hash.copyOfRange(0, EUID.BYTES)))
    }

    fun putSelf(byteBuffer: ByteBuffer) {
        byteBuffer.put(hash)
    }

    fun verifySelf(publicKey: ECPublicKey, signature: ECSignature): Boolean {
        return publicKey.verify(hash, signature)
    }

    fun get(index: Int): Byte {
        return hash[index]
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is RadixHash) {
            return false
        }

        return hash.contentEquals(other.hash)
    }

    override fun hashCode(): Int {
        return BigInteger(hash).hashCode()
    }

    override fun toString(): String {
        return Base64.toBase64String(hash)
    }

    companion object {
        private const val HASH_MAX_SIZE = 12

        @JvmStatic
        fun of(data: ByteArray): RadixHash {
            return RadixHash(Hash.sha256(Hash.sha256(data)))
        }

        @JvmStatic
        fun of(data: ByteArray, offset: Int, length: Int): RadixHash {
            return RadixHash(Hash.sha256(Hash.sha256(data, offset, length)))
        }

        @JvmStatic
        fun sha512of(data: ByteArray): RadixHash {
            return RadixHash(Hash.sha512(Hash.sha512(data)))
        }
    }
}
