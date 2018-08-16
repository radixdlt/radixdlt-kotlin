package com.radixdlt.client.core.pow

import com.radixdlt.client.core.atoms.RadixHash
import okio.ByteString
import java.nio.ByteBuffer
import java.util.*

class ProofOfWorkBuilder {
    fun build(magic: Int, seed: ByteArray, leading: Int): ProofOfWork {
        if (seed.size != 32 || leading < 1 || leading > 256) {
            throw IllegalArgumentException()
        }

        val targetBitSet = BitSet(256)
        targetBitSet.set(0, 256)
        targetBitSet.clear(0, leading / 8 * 8)
        targetBitSet.clear(leading / 8 * 8 + (8 - leading % 8), leading / 8 * 8 + 8)
        val target = targetBitSet.toByteArray()

        val buffer = ByteBuffer.allocate(32 + 4 + 8) // in java8 using java.lang.Long.BYTES

        // Consumable getQuantity cannot be 0 so start at 1
        var nonce: Long = 1
        buffer.putInt(magic)
        buffer.put(seed)

        val targetHex = ByteString.of(*target).hex()

        while (true) {
            buffer.position(32 + 4)
            buffer.putLong(nonce)
            val hashHex = ByteString.of(*RadixHash.of(buffer.array()).toByteArray()).hex()
            if (hashHex.compareTo(targetHex) < 0) {
                return ProofOfWork(nonce, magic, seed, target)
            }
            nonce++
        }
    }
}
