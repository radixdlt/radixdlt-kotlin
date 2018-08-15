package com.radixdlt.client.core.address


enum class RadixUniverseType(private val kname: String, private val ordinalValue: Int) {
    PUBLIC("RADIX_PUBLIC", 1), DEVELOPMENT("RADIX_DEVELOPMENT", 2);

    fun ordinalValue(): Int {
        return ordinalValue
    }

    companion object {
        @JvmStatic
        fun valueOf(ordinalValue: Int): RadixUniverseType {
            for (universeType in RadixUniverseType.values()) {
                if (universeType.ordinalValue == ordinalValue) {
                    return universeType
                }
            }

            throw IllegalArgumentException("No universe type of value: $ordinalValue")
        }
    }
}
