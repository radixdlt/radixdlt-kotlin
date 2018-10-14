package com.radixdlt.client.application.actions

import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.TokenReference
import java.math.BigDecimal
import java.util.Collections
import java.util.HashMap

class TokenTransfer private constructor(
    val from: RadixAddress?,
    val to: RadixAddress?,
    val amount: BigDecimal,
    val tokenReference: TokenReference,
    val attachment: Data?,
    private val metaData: Map<String, Any>
) {

    init {
        if (amount.stripTrailingZeros().scale() > TokenReference.getTokenScale()) {
            throw IllegalArgumentException("Amount must scale by " + TokenReference.getTokenScale())
        }
    }

    fun getMetaData(): Map<String, Any> {
        return Collections.unmodifiableMap(metaData)
    }

    override fun toString(): String {
        val timestamp = metaData["timestamp"] as Long?
        return ("$timestamp $from -> $to $amount ${tokenReference.iso}" +
            if (attachment == null) "" else " $attachment")
    }

    companion object {

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenClass: TokenReference): TokenTransfer {
            return TokenTransfer(from, to, amount, tokenClass, null, emptyMap())
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenReference: TokenReference,
            attachment: Data?
        ): TokenTransfer {
            return TokenTransfer(from, to, amount, tokenReference, attachment, emptyMap())
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenReference: TokenReference,
            timestamp: Long?
        ): TokenTransfer {
            val metaData = HashMap<String, Any>()
            metaData["timestamp"] = timestamp!!

            return TokenTransfer(from, to, amount, tokenReference, null, metaData)
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenReference: TokenReference,
            attachment: Data?,
            timestamp: Long?
        ): TokenTransfer {
            val metaData = HashMap<String, Any>()
            metaData["timestamp"] = timestamp!!

            return TokenTransfer(from, to, amount, tokenReference, attachment, metaData)
        }
    }
}
