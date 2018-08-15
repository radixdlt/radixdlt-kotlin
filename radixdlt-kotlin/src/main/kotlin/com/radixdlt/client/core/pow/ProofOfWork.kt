package com.radixdlt.client.core.pow

import com.radixdlt.client.core.atoms.RadixHash
import okio.ByteString
import org.bouncycastle.util.encoders.Base64

import java.nio.ByteBuffer

class ProofOfWork(val nonce: Long, private val magic: Int, private val seed: ByteArray, private val target: ByteArray) {

    val targetHex: String
        get() = ByteString.of(*target).hex()

    @Throws(ProofOfWorkException::class)
    fun validate() {
        val targetHex = targetHex
        val byteBuffer = ByteBuffer.allocate(4 + 32 + 8) // in java8 using java.lang.Long.BYTES
        byteBuffer.putInt(magic)
        byteBuffer.put(seed)
        byteBuffer.putLong(nonce)
        val hashHex = ByteString.of(*RadixHash.of(byteBuffer.array()).toByteArray()).hex()
        if (hashHex.compareTo(targetHex) > 0) {
            throw ProofOfWorkException(hashHex, targetHex)
        }
    }

    override fun toString(): String {
        return "POW: nonce(" + nonce + ") magic(" + magic + ") seed(" + Base64.toBase64String(seed) + ") target(" + targetHex + ")"
    }
}