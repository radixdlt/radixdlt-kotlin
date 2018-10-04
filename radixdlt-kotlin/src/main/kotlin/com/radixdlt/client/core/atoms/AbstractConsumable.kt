package com.radixdlt.client.core.atoms

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.Dson

abstract class AbstractConsumable internal constructor(
    val quantity: Long,
    val owners: Set<ECKeyPair>?,
    val nonce: Long,
    @field:SerializedName("asset_id")
    val assetId: EUID
) : Particle(1) {
    val destinations: Set<EUID> = owners!!.asSequence().map { it.getUID() }.toSet()

    val ownersPublicKeys: Set<ECPublicKey>
        get() = owners?.asSequence()?.map { it.getPublicKey() }?.toSet() ?: emptySet()

    val isAbstractConsumable: Boolean
        get() = this is AbstractConsumable

    val isConsumable: Boolean
        get() = this is Consumable

    val isConsumer: Boolean
        get() = this is Consumer

    val asConsumer: Consumer
        get() = this as Consumer

    val asConsumable: Consumable
        get() = this as Consumable

    val asAbstractConsumable: AbstractConsumable
        get() = this

    val hash: RadixHash
        get() = RadixHash.of(dson)

    val dson: ByteArray
        get() = Dson.instance.toDson(this)

    abstract val signedQuantity: Long

    override fun toString(): String {
        return "${this.javaClass.name} owners($owners)"
    }
}
