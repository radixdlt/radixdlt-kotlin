package com.radixdlt.client.messaging

import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.EncryptedPayload
import com.radixdlt.client.core.identity.Decryptable

class EncryptedMessage(private val atom: ApplicationPayloadAtom) : Decryptable<RadixMessage> {

    val timestamp: Long
        get() = atom.timestamp

    override fun getEncrypted(): EncryptedPayload? {
        return atom.encryptedPayload
    }

    override fun deserialize(decrypted: ByteArray): RadixMessage {
        return RadixMessage(RadixMessageContent.fromJson(String(decrypted)), atom)
    }

    override fun toString(): String {
        return "Encrypted atom($atom)"
    }

    companion object {
        @JvmStatic
        fun fromAtom(atom: ApplicationPayloadAtom): EncryptedMessage {
            return EncryptedMessage(atom)
        }
    }
}
