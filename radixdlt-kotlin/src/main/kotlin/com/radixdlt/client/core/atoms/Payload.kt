package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.util.Base64Encoded
import org.bouncycastle.util.encoders.Base64
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Temporary class, will remove in the near future
 */
class Payload(
    // TODO: immutable byte array, a copy?
    private val payload: ByteArray?
) : Base64Encoded {

    val bytes: ByteArray
        get() = Arrays.copyOf(payload, payload!!.size)

    fun length(): Int {
        return payload!!.size
    }

    override fun base64(): String {
        return Base64.toBase64String(payload)
    }

    override fun toByteArray(): ByteArray {
        return Arrays.copyOf(payload, payload!!.size)
    }

    fun toUtf8String(): String {
        return String(payload!!, StandardCharsets.UTF_8)
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
