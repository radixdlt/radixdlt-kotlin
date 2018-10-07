package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.util.mergeAfterSum

open class Consumable(
    quantity: Long,
    addresses: List<AccountReference>,
    nonce: Long,
    assetId: EUID,
    planck: Long,
    spin: Long
) : AbstractConsumable(quantity, addresses, nonce, assetId, planck, spin) {

    override val signedQuantity: Long
        get() = if (getSpin() == 1L) amount else if (getSpin() == 2L) -1 * amount else 0

    fun toConsumer(): Consumable {
        return Consumable(super.amount, super.addresses!!, nonce, tokenClass, planck, 2L)
    }

    fun addConsumerQuantities(
        amount: Long,
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
