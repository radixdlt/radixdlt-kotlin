package com.radixdlt.client.core.atoms


class Shards private constructor(private val low: Long, private val high: Long) {

    init {
        if (high < low) {
            throw IllegalArgumentException()
        }
    }

    fun intersects(shards: Collection<Long>): Boolean {
        return shards.asSequence().any { shard -> shard in low..high }
    }

    override fun toString(): String {
        return "[$low, $high]"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Shards) {
            return false
        }

        val s = other as Shards?
        return s!!.high == this.high && s.low == this.low
    }

    override fun hashCode(): Int {
        //TODO: fix HACK
        return (low.toString() + "-" + high).hashCode()
    }

    companion object {
        @JvmStatic
        fun range(low: Long, high: Long): Shards {
            return Shards(low, high)
        }
    }
}