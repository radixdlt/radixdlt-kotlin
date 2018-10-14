package com.radixdlt.client.application.actions

import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.TokenRef
import java.math.BigDecimal
import java.util.Collections
import java.util.HashMap

class TransferTokens private constructor(
    val from: RadixAddress?,
    val to: RadixAddress?,
    val amount: BigDecimal,
    val tokenRef: TokenRef,
    val attachment: Data?,
    private val metaData: Map<String, Any>
) {

    init {
        if (amount.stripTrailingZeros().scale() > TokenRef.getTokenScale()) {
            throw IllegalArgumentException("Amount must scale by " + TokenRef.getTokenScale())
        }
    }

    fun getMetaData(): Map<String, Any> {
        return Collections.unmodifiableMap(metaData)
    }

    override fun toString(): String {
        val timestamp = metaData["timestamp"] as Long?
        return ("$timestamp $from -> $to $amount ${tokenRef.iso}" +
            if (attachment == null) "" else " $attachment")
    }

    companion object {

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenClass: TokenRef): TransferTokens {
            return TransferTokens(from, to, amount, tokenClass, null, emptyMap())
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenReference: TokenRef,
            attachment: Data?
        ): TransferTokens {
            return TransferTokens(from, to, amount, tokenReference, attachment, emptyMap())
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenReference: TokenRef,
            timestamp: Long?
        ): TransferTokens {
            val metaData = HashMap<String, Any>()
            metaData["timestamp"] = timestamp!!

            return TransferTokens(from, to, amount, tokenReference, null, metaData)
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            amount: BigDecimal,
            tokenReference: TokenRef,
            attachment: Data?,
            timestamp: Long?
        ): TransferTokens {
            val metaData = HashMap<String, Any>()
            metaData["timestamp"] = timestamp!!

            return TransferTokens(from, to, amount, tokenReference, attachment, metaData)
        }
    }
}
