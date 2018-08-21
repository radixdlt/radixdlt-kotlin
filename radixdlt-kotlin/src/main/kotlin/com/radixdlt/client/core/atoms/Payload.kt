package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.util.Base64Encoded
import org.bouncycastle.util.encoders.Base64
import java.util.Arrays

class Payload internal constructor(private val payload: ByteArray) : Base64Encoded {

    val bytes: ByteArray
        get() = Arrays.copyOf(payload, payload.size)

    fun length(): Int {
        return payload.size
    }

    override fun base64(): String {
        return Base64.toBase64String(payload)
    }

    override fun toByteArray(): ByteArray {
        return Arrays.copyOf(payload, payload.size)
    }

    fun toAscii(): String {
        return String(payload)
    }

    companion object {

        @JvmStatic
        fun fromBase64(base64Payload: String): Payload {
            return Payload(Base64.decode(base64Payload))
        }

        @JvmStatic
        fun fromAscii(message: String): Payload {
            return Payload(message.toByteArray())
        }
    }
}
