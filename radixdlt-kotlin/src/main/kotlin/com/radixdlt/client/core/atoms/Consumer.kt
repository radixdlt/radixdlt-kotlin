package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair

class Consumer(
    quantity: Long,
    addresses: List<AccountReference>?,
    nonce: Long,
    assetId: EUID,
    planck: Long
) : AbstractConsumable(quantity, addresses, nonce, assetId, planck) {

    override val signedQuantity: Long
        get() = -amount

    fun addConsumerQuantities(amount: Long,
        newOwners: Set<ECKeyPair>,
        consumerQuantities: MutableMap<Set<ECKeyPair>, Long>
    ) {
        if (amount > super.amount) {
            throw IllegalArgumentException("Unable to create consumable with amount $amount (available: ${super.amount})")
        }

        if (amount == super.amount) {
            consumerQuantities.mergeAfterSum(newOwners, amount)
            return
        }

        consumerQuantities.mergeAfterSum(newOwners, amount)
        consumerQuantities.mergeAfterSum(owners, super.amount - amount)
    }
}

fun MutableMap<Set<ECKeyPair>, Long>.mergeAfterSum(key: Set<ECKeyPair>, value: Long): Long {
    val newValue = if (this.containsKey(key)) this.getValue(key) + value else value
    this[key] = newValue
    return newValue
}

fun <K, V> MutableMap<K, V>.mergeAfterFunction(key: K, value: V, function: (t: V, u: V) -> V): V {
    val newValue: V = if (this.containsKey(key)) function(this.getValue(key), value) else value
    this[key] = newValue
    return newValue
}
