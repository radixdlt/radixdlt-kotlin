package com.radixdlt.client.wallet

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.TransactionAtom
import com.radixdlt.client.core.crypto.ECPublicKey
import java.text.SimpleDateFormat
import java.util.*

/**
 * A transaction from the perspective of a wallet. Amount is positive or
 * negative depending on how it affects the balance of the wallet.
 */
class WalletTransaction(address: RadixAddress, val transactionAtom: TransactionAtom) {

    val amount: Long = transactionAtom.particles!!.asSequence()
            .filter{ it.isAbstractConsumable }
            .map { it.asAbstractConsumable }
            .filter { particle -> particle.assetId == Asset.XRD.id }
            .filter { particle -> particle.ownersPublicKeys.all { address.ownsKey(it) } }
            .map { it.signedQuantity }
            .sum()

    val senders: Set<ECPublicKey>
        get() = transactionAtom.summary().entries.asSequence()
                .filter { entry -> entry.value.containsKey(Asset.XRD.id) }
                .filter { entry -> entry.value[Asset.XRD.id]!! < 0 }
                .toList()
                .flatMap { entry -> entry.key }
                .toSet()

    override fun toString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        val txString = StringBuilder(sdf.format(Date(transactionAtom.timestamp))
                + ": " + transactionAtom.summary())

        if (transactionAtom.encrypted != null) { // Note: In java lib we have the method getPayload()
            txString.append(" Payload: " + transactionAtom.encrypted.base64())
        }

        return txString.toString()
    }
}

