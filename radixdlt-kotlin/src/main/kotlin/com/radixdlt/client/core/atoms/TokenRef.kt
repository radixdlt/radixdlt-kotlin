package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.Objects

class TokenRef private constructor(val address: AccountReference, val iso: String) {

    init {
        Objects.requireNonNull(iso)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TokenRef) {
            return false
        }

        return this.iso == other.iso && this.address == other.address
    }

    override fun hashCode(): Int {
        return toString().hashCode() //FIXME: quick hack for now
    }

    override fun toString(): String {
        return String.format("%s/@%s", address.toString(), iso)
    }

    companion object {

        private const val TOKEN_SCALE = 5
        private val CHARSET = StandardCharsets.UTF_8
        const val SUB_UNITS = 100000
        private val SUB_UNITS_BIG_DECIMAL = BigDecimal(SUB_UNITS)

        fun getTokenScale(): Int {
            return TOKEN_SCALE
        }

        fun getSubUnits(): BigDecimal {
            return SUB_UNITS_BIG_DECIMAL
        }

        fun subUnitsToDecimal(subUnits: Long): BigDecimal {
            return BigDecimal.valueOf(subUnits, TOKEN_SCALE)
        }

        fun of(address: AccountReference, reference: String): TokenRef {
            return TokenRef(address, reference)
        }

        fun calcEUID(isoCode: String): EUID {
            return RadixHash.of(isoCode.toByteArray(CHARSET)).toEUID()
        }
    }
}
