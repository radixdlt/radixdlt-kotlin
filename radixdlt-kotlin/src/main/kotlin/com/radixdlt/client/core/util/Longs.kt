package com.radixdlt.client.core.util

import java.util.Objects
import kotlin.experimental.and

object Longs {

    @JvmOverloads
    @JvmStatic
    fun toByteArray(value: Long, bytes: ByteArray = ByteArray(java.lang.Long.BYTES), offset: Int = 0): ByteArray {
        var valueCopy = value
        Objects.requireNonNull(bytes, "bytes is null for 'long' conversion")

        if (offset + LONG_BYTES > bytes.size) {
            throw IllegalArgumentException("bytes is too short for 'long' conversion")
        }

        for (i in LONG_BYTES - 1 downTo 0) {
            bytes[offset + i] = (valueCopy and 0xffL).toByte()
            valueCopy = valueCopy ushr 8
        }

        return bytes
    }

    @JvmStatic
    fun fromByteArray(bytes: ByteArray, offset: Int): Long {
        Objects.requireNonNull(bytes, "bytes is null for 'long' conversion")
        val length = Math.min(bytes.size - offset, java.lang.Long.BYTES)
        if (length <= 0) {
            throw IllegalArgumentException("no bytes for 'long' conversion")
        }

        var value: Long = 0

        for (b in 0 until length) {
            value = value shl 8
            value = value or (bytes[offset + b] and 0xFFL.toByte()).toLong()
        }

        return value
    }
}
