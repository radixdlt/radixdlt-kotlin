package com.radixdlt.client.core.crypto

import com.radixdlt.client.core.util.Base64Encoded
import org.bouncycastle.util.encoders.Base64
import java.util.*

class EncryptedPrivateKey(private val encryptedPrivateKey: ByteArray) : Base64Encoded {

    override fun base64(): String {
        return Base64.toBase64String(encryptedPrivateKey)
    }

    override fun toByteArray(): ByteArray {
        return Arrays.copyOf(encryptedPrivateKey, encryptedPrivateKey.size)
    }
}
