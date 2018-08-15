package com.radixdlt.client.messaging

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.crypto.ECSignature

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Optional
import java.util.TimeZone

class RadixMessage(private val content: RadixMessageContent, private val atom: Atom) {
    val timestamp: Long = atom.timestamp

    val from: RadixAddress
        get() = content.from!!

    val to: RadixAddress
        get() = content.to!!

    fun validateSignature(): Boolean {
        val signature = this.getSignature(this.from.getUID()) ?: return false

        return atom.hash.verifySelf(this.from.publicKey, signature)
    }

    fun getSignature(euid: EUID): ECSignature? {
        return atom.getSignature(euid)
    }

    fun getContent(): String {
        return content.content
    }

    fun createReply(replyContent: String): RadixMessageContent {
        return content.createReply(replyContent)
    }

    override fun toString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(Date(timestamp)) + " " + content.toString()
    }
}
