package com.radixdlt.client.application

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import java.util.ArrayList

/**
 * An Application Layer Action object which stores data in an address.
 */
class StoreDataAction {
    private var data: ByteArray? = null
    private var metaData: Map<String, Any>? = null
    private var protectors: List<EncryptedPrivateKey>? = null
    private val addresses = ArrayList<RadixAddress>()

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

    // TODO: move this to a separate module
    fun addToAtomBuilder(atomBuilder: AtomBuilder) {
        atomBuilder
                .type(ApplicationPayloadAtom::class.java)
                .protectors(protectors!!)
                .payload(data!!)

        if (metaData!!.containsKey("application")) {
            atomBuilder.applicationId(metaData!!["application"] as String)
        }

        addresses.forEach { atomBuilder.addDestination(it) }
    }
}
