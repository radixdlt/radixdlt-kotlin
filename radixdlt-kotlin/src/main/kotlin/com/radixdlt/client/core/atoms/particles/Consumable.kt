package com.radixdlt.client.core.atoms.particles

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.TokenClassReference
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.Dson
import com.radixdlt.client.core.util.mergeAfterSum

open class Consumable(
    val amount: Long,
    val addresses: List<AccountReference>?,
    val nonce: Long,
    tokenId: EUID,
    val planck: Long,
    private val spin: Spin
) : Particle {

    @SerializedName("token_reference")
    private val tokenClassReference: TokenClassReference = TokenClassReference(tokenId, EUID(0))

    fun spinDown(): Consumable {
        return Consumable(
            this.amount,
            addresses,
            nonce,
            getTokenClass(),
            planck,
            Spin.DOWN
        )
    }

    fun addConsumerQuantities(
        amount: Long,
        newOwners: Set<ECKeyPair>,
        consumerQuantities: MutableMap<Set<ECKeyPair>, Long>
    ) {
        if (amount > this.amount) {
            throw IllegalArgumentException(
                "Unable to create consumable with amount $amount (available: ${this.amount})"
            )
        }

        if (amount == this.amount) {
            consumerQuantities.mergeAfterSum(newOwners, amount)
            return
        }

        consumerQuantities.mergeAfterSum(newOwners, amount)
        consumerQuantities.mergeAfterSum(getOwners(), this.amount - amount)
    }

    override fun getSpin(): Spin {
        return spin
    }

    fun getTokenClass(): EUID {
        return tokenClassReference.token
    }

    fun getSignedAmount(): Long {
        return amount * if (getSpin() === Spin.UP) 1 else -1
    }

    override fun getDestinations(): Set<EUID> {
        return getOwnersPublicKeys().asSequence().map { it.getUID() }.toSet()
    }

    fun getOwnersPublicKeys(): Set<ECPublicKey> {
        return addresses?.asSequence()?.map { it.getKey() }?.toSet() ?: emptySet()
    }

    fun getOwners(): Set<ECKeyPair> {
        return getOwnersPublicKeys().asSequence().map { it.toECKeyPair() }.toSet()
    }

    fun getHash(): RadixHash {
        return RadixHash.of(getDson())
    }

    fun getDson(): ByteArray {
        return Dson.instance.toDson(this)
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName} owners($addresses) amount($amount) spin($spin)"
    }
}
