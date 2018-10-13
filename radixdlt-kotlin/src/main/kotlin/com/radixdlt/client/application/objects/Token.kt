package com.radixdlt.client.application.objects

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.RadixHash
import java.nio.charset.StandardCharsets
import java.util.Objects

class Token(val iso: String) {

    val id: EUID = calcEUID(iso)

    init {
        Objects.requireNonNull(iso)
    }

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
        return String.format("%s[%s/%s]", javaClass.simpleName, iso, id)
    }

    companion object {

        private val CHARSET = StandardCharsets.UTF_8
        const val SUB_UNITS = 100000
        /**
         * Radix Token token. TODO: Read from universe file. Hardcode for now.
         */
        @JvmField
        val TEST = Token("XRD")
        @JvmField
        val POW = Token("POW")

        fun calcEUID(isoCode: String): EUID {
            return RadixHash.of(isoCode.toByteArray(CHARSET)).toEUID()
        }
    }
}
