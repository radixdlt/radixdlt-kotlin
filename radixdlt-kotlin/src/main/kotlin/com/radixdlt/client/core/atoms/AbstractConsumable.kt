package com.radixdlt.client.core.atoms

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.TokenClassReference
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.Dson

abstract class AbstractConsumable internal constructor(
    val amount: Long,
    val addresses: List<AccountReference>?,
    val nonce: Long,
    @field:SerializedName("asset_id")
    val tokenReference: EUID,
    val planck: Long
) : Particle {

    private val spin = 1L

    private val tokenClassReference = TokenClassReference(tokenReference, EUID(0))

    override fun getSpin(): Long {
        return spin
    }

    val tokenClass: EUID
        get() = tokenClassReference.token

    override fun getDestinations(): Set<EUID> {
        return ownersPublicKeys.asSequence().map(ECPublicKey::getUID).toSet()
    }

    val ownersPublicKeys: Set<ECPublicKey>
        get() = addresses?.asSequence()?.map(AccountReference::getKey)?.toSet() ?: emptySet()

    val owners: Set<ECKeyPair>
        get() = ownersPublicKeys.asSequence().map(ECPublicKey::toECKeyPair).toSet()

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
        return "${this.javaClass.name} owners($addresses)"
    }
}
