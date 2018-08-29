package com.radixdlt.client.assets

import com.radixdlt.client.core.address.EUID
import java.math.BigInteger
import java.util.Objects

class Asset(val iso: String, val subUnits: Int, val id: EUID) {

    init {
        Objects.requireNonNull(iso)
        Objects.requireNonNull(id)

        if (subUnits == 0) {
            throw IllegalArgumentException("Integer assets should have subUnits set to 1 for mathematical reasons")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Asset) {
            return false
        }

        return this.iso == other.iso
    }

    override fun hashCode(): Int {
        return iso.hashCode()
    }

    companion object {

        /**
         * Radix Token asset. TODO: Read from universe file. Hardcode for now.
         */
        @JvmField
        val TEST = Asset("TEST", 100000, EUID(BigInteger.valueOf("TEST".hashCode().toLong())))
        @JvmField
        val POW = Asset("POW", 1, EUID(BigInteger.valueOf(79416)))
    }
}
