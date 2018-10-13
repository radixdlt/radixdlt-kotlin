package com.radixdlt.client.application.objects

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.RadixHash
import java.nio.charset.StandardCharsets
import java.util.Objects

class Token private constructor(val iso: String, val subUnits: Int, val id: EUID) {

    init {
        Objects.requireNonNull(iso)
        Objects.requireNonNull(id)

        if (subUnits == 0) {
            throw IllegalArgumentException("Integer assets should have subUnits set to 1 for mathematical reasons")
        }
    }

    constructor(iso: String, subUnits: Int) : this(iso, subUnits,
        calcEUID(iso)
    )

    override fun equals(other: Any?): Boolean {
        if (other !is Token) {
            return false
        }

        return this.iso == other.iso
    }

    override fun hashCode(): Int {
        return iso.hashCode()
    }

    override fun toString(): String {
        return String.format("%s[%s/%s/%s]", javaClass.simpleName, iso, subUnits, id)
    }

    companion object {

        private val CHARSET = StandardCharsets.UTF_8
        /**
         * Radix Token token. TODO: Read from universe file. Hardcode for now.
         */
        @JvmField
        val TEST = Token("XRD", 100000)
        @JvmField
        val POW = Token("POW", 1)

        fun calcEUID(isoCode: String): EUID {
            return RadixHash.of(isoCode.toByteArray(CHARSET)).toEUID()
        }
    }
}