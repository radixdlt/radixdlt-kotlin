package com.radixdlt.client.core.util

import java.math.BigInteger

object Base58 {

    private val B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()

    private val R58 = IntArray(256)

    init {
        for (i in 0..255) {
            R58[i] = -1
        }
        for (i in B58.indices) {
            R58[B58[i].toInt()] = i
        }
    }

    // Encodes the specified byte array into a String using the Base58 encoding scheme
    @JvmStatic
    fun toBase58(b: ByteArray): String {
        if (b.size == 0) {
            return ""
        }

        var lz = 0
        while (lz < b.size && b[lz].toInt() == 0) {
            ++lz
        }

        val s = StringBuffer()
        var n = BigInteger(1, b)
        while (n.compareTo(BigInteger.ZERO) > 0) {
            val r = n.divideAndRemainder(BigInteger.valueOf(58))
            n = r[0]
            val digit = B58[r[1].toInt()]
            s.append(digit)
        }
        while (lz > 0) {
            --lz
            s.append("1")
        }
        return s.reverse().toString()
    }

    // Decodes the specified Base58 encoded String to its byte array representation
    @JvmStatic
    fun fromBase58(s: String): ByteArray {
        try {
            var leading = true
            var lz = 0
            var b = BigInteger.ZERO
            for (c in s.toCharArray()) {
                if (leading && c == '1') {
                    ++lz
                } else {
                    leading = false
                    b = b.multiply(BigInteger.valueOf(58))
                    b = b.add(BigInteger.valueOf(R58[c.toInt()].toLong()))
                }
            }
            var encoded = b.toByteArray()
            if (encoded[0].toInt() == 0) {
                if (lz > 0) {
                    --lz
                } else {
                    val e = ByteArray(encoded.size - 1)
                    System.arraycopy(encoded, 1, e, 0, e.size)
                    encoded = e
                }
            }
            val result = ByteArray(encoded.size + lz)
            System.arraycopy(encoded, 0, result, lz, encoded.size)

            return result
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw IllegalArgumentException("Invalid character in myAddress")
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }
    }
}
