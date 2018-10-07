package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.serialization.HasOrdinalValue

enum class Spin(private val value: Int) : HasOrdinalValue {
    UP(1), DOWN(2);

    override fun ordinalValue(): Int {
        return value
    }

    companion object {

        fun valueOf(ordinalValue: Int): Spin {
            return when (ordinalValue) {
                1 -> UP
                2 -> DOWN
                else -> throw IllegalArgumentException("No universe type of value: $ordinalValue")
            }
        }
    }
}
