package com.radixdlt.client.core.network

import com.radixdlt.client.core.atoms.Shards

class NodeRunnerData(val ip: String?, lowShard: Long, highShard: Long) {
    val shards: Shards = Shards.range(lowShard, highShard)

    override fun toString(): String {
        return "$ip: $shards"
    }

    override fun hashCode(): Int {
        // TODO: fix hack
        return (ip + shards.toString()).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is NodeRunnerData) {
            return false
        }

        return other.ip == ip && other.shards == this.shards
    }
}
