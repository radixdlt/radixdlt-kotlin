package com.radixdlt.client.core.atoms.particles

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.Dson
import com.radixdlt.client.core.util.mergeAfterSum

open class Consumable(
    val amount: Long,
    address: AccountReference,
    val nonce: Long,
    @SerializedName("token_reference")
    val tokenRef: TokenRef,
    val planck: Long,
    private val spin: Spin
) : Particle {

    private val addresses: List<AccountReference> = listOf(address)

    val address: AccountReference
        get() = addresses[0]

    override fun getAddresses(): Set<ECPublicKey> {
        return addresses.asSequence().map(AccountReference::getKey).toSet()
    }

    fun spinDown(): Consumable {
        return Consumable(
            this.amount,
            address,
            nonce,
            tokenRef,
            planck,
            Spin.DOWN
        )
    }

    fun addConsumerQuantities(
        amount: Long,
        newOwner: ECKeyPair,
        consumerQuantities: MutableMap<ECKeyPair, Long>
    ) {
        if (amount > this.amount) {
            throw IllegalArgumentException(
                "Unable to create consumable with amount $amount (available: ${this.amount})"
            )
        }

        if (amount == this.amount) {
            consumerQuantities.mergeAfterSum(newOwner, amount)
            return
        }

        consumerQuantities.mergeAfterSum(newOwner, amount)
        consumerQuantities.mergeAfterSum(address.getKey().toECKeyPair(), this.amount - amount)
    }

    override fun getSpin(): Spin {
        return spin
    }

    fun getSignedAmount(): Long {
        return amount * if (getSpin() === Spin.UP) 1 else -1
    }

    fun getOwnersPublicKeys(): Set<ECPublicKey> {
        return addresses.asSequence().map { it.getKey() }.toSet() ?: emptySet()
    }

    fun getOwner(): ECPublicKey {
        return addresses[0].getKey()
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
