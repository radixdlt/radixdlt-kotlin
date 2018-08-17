package com.radixdlt.client.application.actions

import com.radixdlt.client.application.objects.EncryptedData
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import java.util.ArrayList

/**
 * An Application Layer Action object which stores data in an address.
 */
class DataStore {

    // TODO: make this immutable
    val data: ByteArray

    // TODO: make this immutable
    val metaData: Map<String, Any>

    val protectors: List<EncryptedPrivateKey>

    // TODO: make this immutable
    val addresses = ArrayList<RadixAddress>()

    constructor(encryptedData: EncryptedData, address: RadixAddress) {
        this.data = encryptedData.encrypted
        this.metaData = encryptedData.metaData
        this.protectors = encryptedData.protectors

        addresses.add(address)
    }

    constructor(encryptedData: EncryptedData, address0: RadixAddress, address1: RadixAddress) {
        this.data = encryptedData.encrypted
        this.metaData = encryptedData.metaData
        this.protectors = encryptedData.protectors

        addresses.add(address0)
        addresses.add(address1)
    }
}
