package com.radixdlt.client.core.atoms

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair


abstract class AbstractConsumable internal constructor(val quantity: Long, owners: Set<ECKeyPair>, val nonce: Long, @field:SerializedName("asset_id") val assetId: EUID) : Particle(owners.asSequence().map(ECKeyPair::getUID).toSet(), owners) {
    abstract val signedQuantity: Long
}
