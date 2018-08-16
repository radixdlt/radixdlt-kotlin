package com.radixdlt.client.core.util

import java.lang.Long.MIN_VALUE
import java.util.*

/**
 * A 128-bit signed integer, with comparison and some basic arithmetic
 * operations.
 */
class Int128 private constructor(val high: Long, val low: Long) : Number(), Comparable<Int128> {

    /**
     * Convert value to an array of bytes.
     * The most significant byte will be returned in index zero.
     * The array will always be [.BYTES] bytes long, and
     * will be zero filled to suit the actual value.
     *
     * @return An array of [.BYTES] bytes representing the
     * value of this [Int128].
     */
    fun toByteArray(): ByteArray {
        return toByteArray(ByteArray(BYTES), 0)
    }

    /**
     * Convert value to an array of bytes.
     * The most significant byte will be returned in index `offset`.
     * The array must be at least `offset + BYTES` long.
     *
     * @param bytes The array to place the bytes in.
     * @return The passed-in value of `bytes`.
     */
    fun toByteArray(bytes: ByteArray, offset: Int): ByteArray {
        Longs.toByteArray(this.high, bytes, offset)
        Longs.toByteArray(this.low, bytes, offset + LONG_BYTES)
        return bytes
    }

    /**
     * Add `other` to `this`, returning the result.
     *
     * @param other The addend.
     * @return An [Int128] with the value `this + other`.
     */
    fun add(other: Int128): Int128 {
        val newLow = this.low + other.low
        // Hacker's Delight section 2-13:
        // "The following branch-free code can be used to compute the
        // overflow predicate for unsigned add/subtract, with the result
        // being in the sign position."
        val carry = if (this.low.ushr(1) + other.low.ushr(1) + (this.low and other.low and 1) < 0) 1L else 0L
        val newHigh = this.high + other.high + carry
        return Int128.from(newHigh, newLow)
    }

    /**
     * Subtract `other` from `this`, returning the result.
     *
     * @param other The subtrahend.
     * @return An [Int128] with the value `this - other`.
     */
    fun subtract(other: Int128): Int128 {
        val newLow = this.low - other.low
        // Hacker's Delight section 2-13:
        // "The following branch-free code can be used to compute the
        // overflow predicate for unsigned add/subtract, with the result
        // being in the sign position."
        val carry = if (this.low.ushr(1) - other.low.ushr(1) - (this.low.inv() and other.low and 1) < 0) 1L else 0L
        val newHigh = this.high - other.high - carry
        return Int128.from(newHigh, newLow)
    }

    /**
     * Multiply `this` by the specified multiplicand.
     *
     * @param multiplicand The multiplicand to multiply `this` by.
     * @return The result `this * multiplicand`.
     */
    fun multiply(multiplicand: Int128): Int128 {
        var multiplicandCopy = multiplicand
        // Russian peasant
        var result = Int128.ZERO
        var multiplier = this

        while (!multiplicandCopy.isZero()) {
            if (multiplicandCopy.isOdd()) {
                result = result.add(multiplier)
            }

            multiplier = multiplier.shiftLeft()
            multiplicandCopy = multiplicandCopy.logicalShiftRight()
        }
        return result
    }

    /**
     * Divide `this` by the specified divisor.
     *
     * @param divisor The divisor to divide `this` by.
     * @return The result `floor(this / divisor)`.
     */
    fun divide(divisor: Int128): Int128 {
        var divisorCopy = divisor
        val negative = this.high xor divisorCopy.high < 0
        var dividend = this.abs()
        divisorCopy = divisorCopy.abs()
        var sum = Int128.ZERO
        var cmp = dividend.compareToUnsigned(divisorCopy)
        while (cmp >= 0) {
            var quotient = Int128.ONE
            var div = divisorCopy
            var next = divisorCopy.shiftLeft()
            while (next.compareToUnsigned(dividend) <= 0) {
                div = next
                next = next.shiftLeft()
                quotient = quotient.shiftLeft()
            }
            sum = sum.add(quotient)
            dividend = dividend.subtract(div)
            cmp = dividend.compareToUnsigned(divisorCopy)
        }
        return if (negative) sum.negate() else sum
    }

    /**
     * Return the remainder of the division of `this` by
     * the specified divisor.
     *
     * @param divisor The divisor to divide `this` by.
     * @return The remainder of the division `this / divisor`.
     */
    fun remainder(divisor: Int128): Int128 {
        val negative = this.high < 0
        val result = remainder(this.abs(), divisor.abs())
        return if (negative) result.negate() else result
    }

    /**
     * Shift `this` left 1 bit.  A zero bit is moved into the
     * leftmost bit.
     *
     * @return The result of shifting `this` left one bit.
     */
    fun shiftLeft(): Int128 {
        val h = this.high shl 1 or this.low.ushr(java.lang.Long.SIZE - 1)
        val l = this.low shl 1
        return Int128.from(h, l)
    }

    /**
     * Arithmetic shift `this` right 1 bit.  The current value
     * of the sign bit is duplicated into the rightmost bit.
     *
     * @return The result of arithmetic shifting `this` right one bit.
     */
    fun shiftRight(): Int128 {
        val h = this.high shr 1
        val l = this.low.ushr(1) or (this.high shl java.lang.Long.SIZE - 1)
        return Int128.from(h, l)
    }

    /**
     * Logical shift `this` right 1 bit.  Zeros are shifted into
     * the rightmost bit.
     *
     * @return The result of logical shifting `this` right one bit.
     */
    fun logicalShiftRight(): Int128 {
        val h = this.high.ushr(1)
        val l = this.low.ushr(1) or (this.high shl java.lang.Long.SIZE - 1)
        return Int128.from(h, l)
    }

    /**
     * Similar to [.shiftRight], but rounds towards zero.
     *
     * @return The result of calculating `this / 2`.
     */
    fun div2(): Int128 {
        var l = this.low
        var h = this.high
        if (h < 0) {
            l += 1
            if (l == 0L) {
                h += 1
            }
        }
        return Int128.from(h shr 1, l.ushr(1) or (h shl java.lang.Long.SIZE - 1))
    }

    /**
     * Return the absolute value of `this`.
     *
     *
     * Note that, similarly to other two's complement numbers, the
     * absolute value of the  maximal negative value is returned as
     * itself.
     *
     * @return The absolute value of `this`.
     */
    fun abs(): Int128 {
        return if (this.high < 0) this.negate() else this
    }

    /**
     * Return the value of `0 - this`.
     *
     *
     * Note that, similarly to other two's complement numbers, the
     * negative value of the maximal negative value is returned as
     * itself.
     *
     * @return The negative value of `this`.
     */
    fun negate(): Int128 {
        // Two's complement
        var h = this.high.inv()
        var l = this.low.inv()
        l += 1
        if (l == 0L) {
            h += 1
        }
        return Int128.from(h, l)
    }

    override fun compareTo(other: Int128): Int {
        var cmp = java.lang.Long.compare(this.high, other.high)
        if (cmp == 0) {
            cmp = compareUnsigned(this.low, other.low)
        }
        return cmp
    }

    /**
     * Compares `this` and `n` numerically treating the values
     * as unsigned.
     *
     * @param  n the second [Int128] to compare.
     * @return the value `0` if `this == n`; a value less
     * than `0` if `this < n` as unsigned values; and
     * a value greater than `0` if `this > n` as
     * unsigned values
     */
    fun compareToUnsigned(n: Int128): Int {
        var cmp = compareUnsigned(this.high, n.high)
        if (cmp == 0) {
            cmp = compareUnsigned(this.low, n.low)
        }
        return cmp
    }

    /**
     * Compares two `long` values numerically treating the values
     * as unsigned.
     *
     * @param  x the first `long` to compare
     * @param  y the second `long` to compare
     * @return the value `0` if `x == y`; a value less
     * than `0` if `x < y` as unsigned values; and
     * a value greater than `0` if `x > y` as
     * unsigned values
     *
     * copied from Java8 Long.class and converted to Kotlin
     */
    private fun compareUnsigned(x: Long, y: Long): Int {
        return java.lang.Long.compare(x + MIN_VALUE, y + MIN_VALUE)
    }

    override fun toInt(): Int {
        return low.toInt()
    }

    override fun toLong(): Long {
        return low
    }

    override fun toFloat(): Float {
        return toDouble().toFloat()
    }

    override fun toDouble(): Double {
        // If it's a number that fits into a long, let the compiler
        // convert.
        if (this.high == 0L && this.low >= 0L || this.high == -1L && this.low < 0L) {
            return low.toDouble()
        }

        var h = this.high
        var l = this.low
        val negative = h < 0
        if (negative) {
            // Calculate two's complement
            h = h.inv()
            l = l.inv()
            l += 1
            if (l == 0L) {
                h += 1
            }
        }

        // Must be at least 64 bits based on initial checks.
        // Note that it is not possible for this exponent to overflow a double
        // (128 < 1023).
        val shift = bitLength(h)
        var exponent = (java.lang.Long.SIZE + shift - 1).toLong()

        // Merge all the bits into l, discarding lower bits
        l = l ushr shift.toLong().toInt()
        h = h shl (java.lang.Long.SIZE - shift).toLong().toInt()
        l = l or h

        // Extract 53 bits of significand. Note that we make a
        // quick stop part way through to organise rounding.
        // Note that rounding is approximate, not RTNE.
        l = l ushr (java.lang.Long.SIZE - SIGNIFICAND_PREC - 1).toLong().toInt()
        l += 1
        l = l ushr 1

        // If rounding has caused overflow, then shift an extra bit
        if (l and SIGNIFICAND_OVF != 0L) {
            exponent += 1
            l = l ushr 1
        }

        // Assemble into a double now.
        var raw = exponent + EXPONENT_BIAS shl SIGNIFICAND_PREC - 1
        raw = raw or (l and SIGNIFICAND_MASK)
        val value = java.lang.Double.longBitsToDouble(raw)
        return if (negative) -1.0 * value else value
    }

    override fun toByte(): Byte {
        return toInt().toByte()
    }

    override fun toChar(): Char {
        return toInt().toChar()
    }

    override fun toShort(): Short {
        return toInt().toShort()
    }

    /**
     * Calculates the bitwise inclusive-or of `this` with `other`
     * (`this | other`).
     *
     * @param other The value to inclusive-or with `this`.
     * @return `this | other`
     */
    fun or(other: Int128): Int128 {
        return Int128.from(this.high or other.high, this.low or other.low)
    }

    /**
     * Calculates the bitwise and of `this` with `other`
     * (`this & other`).
     *
     * @param other The value to and with `this`.
     * @return `this & other`
     */
    fun and(other: Int128): Int128 {
        return Int128.from(this.high and other.high, this.low and other.low)
    }

    /**
     * Calculates the exclusive-or of `this` with `other`
     * (`this ^ other`).
     *
     * @param other The value to exclusive-or with `this`.
     * @return `this ^ other`
     */
    fun xor(other: Int128): Int128 {
        return Int128.from(this.high xor other.high, this.low xor other.low)
    }

    /**
     * Returns the number of zero bits preceding the highest-order
     * ("leftmost") one-bit in the two's complement binary representation
     * of the specified `long` value.  Returns 128 if the
     * specified value has no one-bits in its two's complement representation,
     * in other words if it is equal to zero.
     *
     *
     * Note that this method is closely related to the logarithm base 2.
     * For all positive `long` values x:
     *
     *  * floor(log<sub>2</sub>(x)) = `127 - numberOfLeadingZeros(x)`
     *  * ceil(log<sub>2</sub>(x)) = `128 - numberOfLeadingZeros(x - 1)`
     *
     *
     * @return the number of zero bits preceding the highest-order
     * ("leftmost") one-bit in the two's complement binary representation
     * of the specified `long` value, or 128 if the value
     * is equal to zero.
     */
    fun numberOfLeadingZeros(): Int {
        return if (this.high == 0L)
            java.lang.Long.SIZE + java.lang.Long.numberOfLeadingZeros(this.low)
        else
            java.lang.Long.numberOfLeadingZeros(this.high)
    }

    /**
     * Return `true` if `this` is zero.
     *
     * @return `true` if `this` is zero.
     */
    fun isZero(): Boolean {
        return this.high == 0L && this.low == 0L
    }

    /**
     * Return `true` if `this` is an even number.
     *
     * @return `true` if `this` is an even number.
     */
    fun isEven(): Boolean {
        return this.low and 1L == 0L
    }

    /**
     * Return `true` if `this` is an odd number.
     *
     * @return `true` if `this` is an odd number.
     */
    fun isOdd(): Boolean {
        return this.low and 1L != 0L
    }

    override fun hashCode(): Int {
        return java.lang.Long.hashCode(this.high) * 31 + java.lang.Long.hashCode(this.low)
    }

    override fun equals(other: Any?): Boolean {
        // Note that this needs to be consistent with compareTo
        if (this === other) {
            return true
        }

        if (other is Int128) {
            return this.high == other.high && this.low == other.low
        }
        return false
    }

    override fun toString(): String {
        if (isZero()) {
            return "0"
        }

        // This is cheating to avoid the case where this.abs() < 0
        // In Kotlin we need method to deal with unsigned 64-bit integers.
        // https://discuss.kotlinlang.org/t/long-type-hex-error/4310/4
        if (this.high == unsignedLong(0x8000_0000, 0x0000_0000) && this.low == 0L) {
            return "-170141183460469231731687303715884105728"
        }

        val sb = StringBuilder()
        val negative = this.high < 0
        var n = this.abs()
        while (!n.isZero()) {
            val digit = n.remainder(Int128.TEN)
            sb.append(('0'.toLong() + digit.low).toChar())
            n = n.divide(Int128.TEN)
        }

        if (negative) {
            sb.append('-')
        }
        return sb.reverse().toString()
    }

    private fun unsignedLong(mostSignificantBits: Long, leastSignificantBits: Long) =
            (mostSignificantBits shl 32) or leastSignificantBits


    companion object {
        private val serialVersionUID = 8627474700385282074L

        // Constants used for doubleValue()
        // Values taken from
        // https://en.wikipedia.org/wiki/Double-precision_floating-point_format
        private val SIGNIFICAND_PREC = 53 // Including implicit leading one bit
        private val SIGNIFICAND_MASK = (1L shl SIGNIFICAND_PREC - 1) - 1L
        private val SIGNIFICAND_OVF = 1L shl SIGNIFICAND_PREC
        private val EXPONENT_BIAS = 1023

        // Some sizing constants in line with Integer, Long etc
        /**
         * Size of this numeric type in bits.
         */
        val SIZE = java.lang.Long.SIZE * 2
        /**
         * Size of this numeric type in bytes.
         */
        val BYTES = LONG_BYTES * 2

        // Some commonly used values
        val ZERO = Int128(0L, 0L)
        val ONE = Int128(0L, 1L)
        val TWO = Int128(0L, 2L)
        val THREE = Int128(0L, 3L)
        val FOUR = Int128(0L, 4L)
        val FIVE = Int128(0L, 5L)
        val SIX = Int128(0L, 6L)
        val SEVEN = Int128(0L, 7L)
        val EIGHT = Int128(0L, 8L)
        val NINE = Int128(0L, 9L)
        val TEN = Int128(0L, 10L)
        val MINUS_ONE = Int128(-1L, -1L)

        // Numbers in order.  This is used by factory methods.
        private val NUMBERS = arrayOf(ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN)

        /**
         * Factory method for materialising an [Int128] from a `short`
         * value.
         *
         * @param value The value to be represented as an [Int128].
         * @return `value` as an [Int128] type.
         */
        fun from(value: Short): Int128 {
            return from(value.toLong())
        }

        /**
         * Factory method for materialising an [Int128] from an `int` value.
         *
         * @param value The value to be represented as an [Int128].
         * @return `value` as an [Int128] type.
         */
        fun from(value: Int): Int128 {
            return from(value.toLong())
        }

        /**
         * Factory method for materialising an [Int128] from a `long` value.
         * Note that values are sign extended into the 128 bit value.
         *
         * @param value The value to be represented as an [Int128].
         * @return `value` as an [Int128] type.
         */
        fun from(value: Long): Int128 {
            // Sign extend
            return from(if (value < 0) -1L else 0L, value)
        }

        /**
         * Factory method for materialising an [Int128] from two `long`
         * values. `high` is the most significant word, and `low` the least
         * significant.
         *
         * @param high The most significant word of the 128 bit value.
         * @param low  The least significant word of the 128 bit value.
         * @return `(high << 64) | low` as an [Int128] type.
         */
        fun from(high: Long, low: Long): Int128 {
            if (high == 0L) {
                if (low >= 0L && low < NUMBERS.size) {
                    return NUMBERS[low.toInt()]
                }
            } else if (high == -1L) {
                if (low == -1L) {
                    return MINUS_ONE
                }
            }
            return Int128(high, low)
        }

        /**
         * Factory method for materialising an [Int128] from an array
         * of bytes.  The array is most-significant byte first, and must not be
         * zero length or greater than [.BYTES] bytes long.
         * If the array is smaller than [.BYTES], then it is effectively
         * padded with leading bytes with the correct sign.
         *
         * @param bytes The array of bytes to be used.
         * @return `bytes` as an [Int128] type.
         * @throws IllegalArgumentException if `bytes` is not exactly
         * [.BYTES] bytes in length.
         * @see .toByteArray
         */
        fun from(bytes: ByteArray): Int128 {
            Objects.requireNonNull(bytes)
            if (bytes.isEmpty() || bytes.size > BYTES) {
                throw IllegalArgumentException(String.format("bytes is not %s bytes long: %s", BYTES, bytes.size))
            }

            val newBytes = extend(bytes)
            val high = Longs.fromByteArray(newBytes, 0)
            val low = Longs.fromByteArray(newBytes, LONG_BYTES)
            return Int128.from(high, low)
        }

        /**
         * Factory method for materialising an [Int128] from a string.
         * Conversion is performed base 10 and leading sign characters are
         * permitted.
         *
         * @param s The array of bytes to be used.
         * @return `s` as an [Int128] type.
         * @throws NumberFormatException if `s` is not a valid
         * integer number.
         */
        fun from(s: String): Int128 {
            Objects.requireNonNull(s)

            val len = s.length
            if (len > 0) {
                var i = 0
                var negative = false
                val ch = s[0]
                if (ch == '-') {
                    negative = true
                    i += 1 // skip first char
                } else if (ch == '+') {
                    i += 1 // skip first char
                }
                if (i >= len) {
                    throw NumberFormatException(s)
                }
                // No real effort to catch overflow here
                var result = Int128.ZERO
                while (i < len) {
                    val digit = Character.digit(s[i++], 10)
                    if (digit < 0) {
                        throw NumberFormatException(s)
                    }
                    result = result.multiply(Int128.TEN).add(NUMBERS[digit])
                }
                return if (negative) result.negate() else result
            } else {
                throw NumberFormatException(s)
            }
        }

        // Pad short (< BYTES length) array with appropriate lead bytes.
        private fun extend(bytes: ByteArray): ByteArray {
            if (bytes.size >= BYTES) {
                return bytes
            }

            val newBytes = ByteArray(BYTES)
            val newPos = BYTES - bytes.size
            // Sign extension
            Arrays.fill(newBytes, 0, newPos, if (bytes[0] < 0) 0xFF.toByte() else 0x00.toByte())
            System.arraycopy(bytes, 0, newBytes, newPos, bytes.size)
            return newBytes
        }

        private fun bitLength(n: Long): Int {
            return java.lang.Long.SIZE - java.lang.Long.numberOfLeadingZeros(n)
        }

        // Internal computation.  dividend and divisor assumed positive.
        private fun remainder(dividend: Int128, divisor: Int128): Int128 {
            var dividendCopy = dividend
            while (true) {
                val cmp = dividendCopy.compareToUnsigned(divisor)
                if (cmp == 0) {
                    return Int128.ZERO
                } else if (cmp < 0) {
                    return dividendCopy
                }

                var div = divisor
                var next = divisor.shiftLeft()
                while (next.compareToUnsigned(dividendCopy) <= 0) {
                    div = next
                    next = next.shiftLeft()
                }
                dividendCopy = dividendCopy.subtract(div)
            }
        }
    }
}