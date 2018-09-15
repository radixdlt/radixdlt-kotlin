package com.radixdlt.client.core.serialization

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Serialisation encoding / decoding utilities.
 */
class SerializationUtils private constructor() {

    init {
        throw IllegalArgumentException("Can't construct")
    }

    companion object {

        /**
         * The maximum value we can serialize as a length.
         * Numbers `[0..SERIALIZE_MAX_INT]` can be
         * serialized by [.intLength],
         * [.encodeInt] and
         * [.decodeInt].
         *
         *
         * Currently this value is 2<sup>29</sup>-1.
         */
        internal val SERIALIZE_MAX_INT = (1 shl (5 + java.lang.Byte.SIZE * 3)) - 1

        /**
         * Return the length in bytes of `value` when encoded.
         *
         *
         * See [.encodeInt] for details of the encoding
         * scheme.
         *
         * @param value The value to be encoded.
         * @return The number of bytes required to encode `value`.
         * @throws IllegalArgumentException if {@value} is outside the range of
         * values that can be encoded.
         * @see .encodeInt
         */
        @JvmStatic
        internal fun intLength(value: Int): Int {
            if (value < 0 || value > SERIALIZE_MAX_INT) {
                throw IllegalArgumentException("Invalid length: $value")
            }
            // This code is quicker than using a lookup table based on
            // Integer.numerOfLeadingZeros(value) in case you were wondering.
            if (value < 0xA0) { // Numbers less than 160 as is
                return 1
            } else if (value < 1 shl 5 + java.lang.Byte.SIZE) { // 13 bit numbers in 2 bytes
                return 2
            } else if (value < 1 shl 5 + java.lang.Byte.SIZE * 2) { // 21 bit numbers in 3 bytes
                return 3
            }
            // Max 29 bit numbers
            return 4
        }

        /**
         * Encode the specified `value` into a variable number of octets.
         *
         *
         * The encoding scheme is given by:
         *
         *  1. If `value` is less than 2<sup>7</sup>, encode it in a
         * single byte.
         *  1. Otherwise, if `value` is less than 2<sup>13</sup>, the
         * most significant 5 bits are encoded on the first byte, ored with
         * `0x80`, and the remaining 8 bits are encoded on the second
         * byte.
         *  1. Otherwise, if `value` is less than 2<sup>21</sup>, the
         * most significant 5 bits are encoded on the first byte, ored with
         * `0xA0`, the next most significant 8 bits on the next byte,
         * followed by the least significant 8 bits on the final byte.
         *  1. Otherwise, if `value` is less than 2<sup>29</sup>, the
         * most significant 5 bits are encoded on the first byte, ored with
         * `0xC0`, the next most significant 8 bits on the next byte,
         * and so on for the remaining 3 bytes.
         *
         * Values outside the range [0, 2<sup>29</sup>-1] cannot be
         * encoded by this method and will throw a [IllegalStateException].
         *
         * @param value  The value to be encoded.
         * @param bytes  The byte buffer to encode the value into.
         */
        @JvmStatic
        internal fun encodeInt(value: Int, bytes: ByteBuffer) {
            when (intLength(value)) {
                1 -> bytes.put(value.toByte())
                2 -> {
                    bytes.put((value shr 8 and 0x1F or 0xA0).toByte())
                    bytes.put(value.toByte())
                }
                3 -> {
                    bytes.put((value shr 16 and 0x1F or 0xC0).toByte())
                    bytes.putShort(value.toShort())
                }
                4 -> {
                    bytes.put((value shr 24 and 0x1F or 0xE0).toByte())
                    bytes.put((value shr 16).toByte())
                    bytes.putShort(value.toShort())
                }
                else ->
                    // Should not be able to get here - programming logic issue
                    throw IllegalArgumentException("Invalid length: $value")
            }
        }

        /**
         * Decode a variable-length value from a [ByteBuffer].
         *
         *
         * See [.encodeInt] for details of the encoding
         * scheme.
         *
         * @param bytes The byte buffer from which to decode.
         * @return The decoded integer.
         */
        @JvmStatic
        internal fun decodeInt(bytes: ByteBuffer): Int {
            val b0: Int = bytes.get().toInt() and 0xFF
            when (b0 shr 5) {
                0, 1, 2, 3, 4 -> return b0
                5 -> {
                    val b1: Int = bytes.get().toInt() and 0xFF
                    return b0 and 0x1F shl 8 or b1
                }
                6 -> {
                    val s1: Int = bytes.short.toInt() and 0xFFFF
                    return b0 and 0x1F shl 16 or s1
                }
                7 -> {
                    val s2: Int = bytes.short.toInt() and 0xFFFF
                    val b2: Int = bytes.get().toInt() and 0xFF
                    return b0 and 0x1F shl 24 or (s2 shl 8) or b2
                }
                else ->
                    // Should not be able to get here - programming logic issue
                    throw IllegalArgumentException(String.format("Can't decode lead byte of %02X", b0))
            }
        }

        /**
         * Write an encoded integer to a [ByteArrayOutputStream].
         *
         * @param value The integer to write
         * @param outputStream The output stream to write on
         * @see .encodeInt
         */
        @JvmStatic
        fun encodeInt(length: Int, outputStream: ByteArrayOutputStream) {
            val bytes = ByteArray(0x10) // Greater than maximum size of encoded number
            val buf = ByteBuffer.wrap(bytes)
            encodeInt(length, buf)
            outputStream.write(bytes, 0, buf.position())
        }
    }
}
