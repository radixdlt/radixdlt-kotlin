package com.radixdlt.client.core.address

import com.radixdlt.client.core.util.Int128
import org.bouncycastle.util.encoders.Hex
import java.util.Arrays

class EUID {
    private val value: Int128

    val shard: Long
        get() = value.high

    constructor(value: Int128) {
        this.value = value
    }

    constructor(bytes: ByteArray) {
        var bytesCopy = bytes
        if (bytesCopy.size > BYTES) {
            bytesCopy = Arrays.copyOf(bytesCopy, BYTES)
        }
        this.value = Int128.from(bytesCopy)
    }

    constructor(value: Int) {
        this.value = Int128.from(value)
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EUID) {
            return false
        }

        return this.value == other.value
    }

    override fun toString(): String {
        return Hex.toHexString(value.toByteArray())
    }

    /**
     * Return an array of `byte` that represents this [EUID].
     * Note that the returned array is always [.BYTES] bytes in length,
     * and is padded on the right with the value of the sign bit, if necessary.
     *
     * @return An array of [.BYTES] bytes.
     */
    fun toByteArray(): ByteArray {
        val bytes = value.toByteArray()
        if (bytes.size < BYTES) {
            // Pad with sign bit
            val newBytes = ByteArray(BYTES)
            val fillSize = BYTES - bytes.size
            val fill = if (bytes[0] < 0) (-1).toByte() else 0.toByte()
            Arrays.fill(newBytes, 0, fillSize, fill)
            System.arraycopy(bytes, 0, newBytes, fillSize, bytes.size)
            return newBytes
        }
        return bytes
    }

    companion object {
        const val BYTES = Int128.BYTES
    }
}
