package com.radixdlt.client.core.serialization

/**
 * Hack to get DSON working with Asset Atoms for now
 */
interface HasOrdinalValue {
    fun ordinalValue(): Int
}
