package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair

class Consumer : AbstractConsumable {

    override val signedQuantity: Long
        get() = -quantity

    constructor(quantity: Long, owner: ECKeyPair, nonce: Long, assetId: EUID) : super(quantity, setOf<ECKeyPair>(owner), nonce, assetId)

    constructor(quantity: Long, owners: Set<ECKeyPair>, nonce: Long, assetId: EUID) : super(quantity, owners, nonce, assetId)


    fun addConsumerQuantities(amount: Long, newOwners: Set<ECKeyPair>, consumerQuantities: MutableMap<Set<ECKeyPair>, Long>) {
        if (amount > quantity) {
            throw IllegalArgumentException("Unable to create consumable with amount $amount (available: $quantity)")
        }

        if (amount == quantity) {
            consumerQuantities.mergeAfterSum(newOwners, amount)
            return
        }

        consumerQuantities.mergeAfterSum(newOwners, amount)
        consumerQuantities.mergeAfterSum(owners!!, quantity - amount)
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
