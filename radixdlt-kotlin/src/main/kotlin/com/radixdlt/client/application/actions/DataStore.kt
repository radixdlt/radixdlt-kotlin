package com.radixdlt.client.application.actions

import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.address.RadixAddress
import java.util.Arrays
import java.util.Collections

/**
 * An Application Layer Action object which stores data into an address or multiple addresses.
 */
class DataStore {
    val data: Data
    private val addresses: List<RadixAddress>

    constructor(data: Data, address: RadixAddress) {
        this.data = data
        this.addresses = listOf(address)
    }

    constructor(data: Data, address0: RadixAddress, address1: RadixAddress) {
        this.data = data
        this.addresses = Arrays.asList(address0, address1)
    }

    fun getAddresses(): List<RadixAddress> {
        return Collections.unmodifiableList(addresses)
    }
}
