package com.radixdlt.client.application.actions

import com.radixdlt.client.core.address.RadixAddress
import java.util.Objects

/**
 * A property attached to a transaction which adds the constraint that no
 * other transaction will be accepted by the network with the same unique
 * bytes and address.
 */
class UniqueProperty(
    // TODO: make byte array immutable
    val unique: ByteArray,
    val address: RadixAddress
) {

    init {
        Objects.requireNonNull(unique)
        Objects.requireNonNull(address)
    }
}
