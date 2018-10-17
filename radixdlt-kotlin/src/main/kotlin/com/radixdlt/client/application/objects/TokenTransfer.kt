package com.radixdlt.client.application.objects

import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress

class TokenTransfer(
    val from: RadixAddress,
    val to: RadixAddress,
    val tokenClass: Asset,
    val subUnitAmount: Long,
    val attachment: UnencryptedData?,
    val timestamp: Long
) {

    val attachmentAsString: String?
          get() = attachment?.data?.let { String(it) }

    override fun toString(): String {
        return ("$timestamp $from -> $to " + Amount.subUnitsOf(subUnitAmount, tokenClass).toString()
            + if (attachment == null) "" else " $attachment")
    }
}
