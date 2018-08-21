package com.radixdlt.client.application.actions

import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.assets.AssetAmount
import com.radixdlt.client.core.address.RadixAddress
import java.util.Collections
import java.util.HashMap

class TokenTransfer private constructor(
    val from: RadixAddress?,
    val to: RadixAddress?,
    val tokenClass: Asset,
    val subUnitAmount: Long,
    val attachment: Data?,
    private val metaData: Map<String, Any>
) {

    fun getMetaData(): Map<String, Any> {
        return Collections.unmodifiableMap(metaData)
    }

    override fun toString(): String {
        val timestamp = metaData["timestamp"] as Long
        return ("$timestamp $from -> $to ${AssetAmount(
            tokenClass,
            subUnitAmount
        )}${if (attachment == null) "" else " $attachment"}")
    }

    companion object {

        @JvmStatic
        fun create(from: RadixAddress?, to: RadixAddress?, tokenClass: Asset, subUnitAmount: Long): TokenTransfer {
            return TokenTransfer(from, to, tokenClass, subUnitAmount, null, emptyMap())
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            tokenClass: Asset,
            subUnitAmount: Long,
            attachment: Data?
        ): TokenTransfer {
            return TokenTransfer(from, to, tokenClass, subUnitAmount, attachment, emptyMap())
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            tokenClass: Asset,
            subUnitAmount: Long,
            timestamp: Long?
        ): TokenTransfer {
            val metaData = HashMap<String, Any>()
            metaData["timestamp"] = timestamp!!

            return TokenTransfer(from, to, tokenClass, subUnitAmount, null, metaData)
        }

        @JvmStatic
        fun create(
            from: RadixAddress?,
            to: RadixAddress?,
            tokenClass: Asset,
            subUnitAmount: Long,
            attachment: Data?,
            timestamp: Long?
        ): TokenTransfer {
            val metaData = HashMap<String, Any>()
            metaData["timestamp"] = timestamp!!

            return TokenTransfer(from, to, tokenClass, subUnitAmount, attachment, metaData)
        }
    }
}
