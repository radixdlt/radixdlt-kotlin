package com.radixdlt.client.core.address

import java.math.BigInteger

class EUID {
    private val value: BigInteger

    val shard: Long
        get() = value.toLong()

    constructor(value: ByteArray) {
        this.value = BigInteger(value)
    }

    constructor(value: BigInteger) {
        this.value = value
    }

    fun bigInteger(): BigInteger {
        return value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is EUID) {
            return false
        }

        val o = other as EUID?
        return this.value == o!!.value
    }

    override fun toString(): String {
        return value.toString()
    }
}
