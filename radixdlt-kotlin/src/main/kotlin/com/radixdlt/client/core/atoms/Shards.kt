package com.radixdlt.client.core.atoms

class Shards private constructor(private val low: Long, private val high: Long) {

    init {
        if (high < low) {
            throw IllegalArgumentException()
        }
    }

    fun intersects(shards: Collection<Long>): Boolean {
        return shards.asSequence().any(this::contains)
    }

    operator fun contains(shard: Long): Boolean {
        return shard in low..high
    }

    override fun toString(): String {
        return "[$low, $high]"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Shards) {
            return false
        }

        return other.high == this.high && other.low == this.low
    }

    override fun hashCode(): Int {
        return hashCode(this.high) * 31 + hashCode(this.low)
    }

    /**
     * Returns a hash code for a `long` value.
     *
     * @param value the value to hash
     * @return a hash code value for a `long` value.
     */
    private fun hashCode(value: Long): Int = (value xor value.ushr(32)).toInt()

    companion object {
        @JvmStatic
        fun range(low: Long, high: Long): Shards {
            return Shards(low, high)
        }
    }
}
