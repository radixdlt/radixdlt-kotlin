package com.radixdlt.client.core.crypto

import org.bouncycastle.util.encoders.Base64

import java.math.BigInteger

class ECSignature(r: BigInteger, s: BigInteger) {
    private val r: ByteArray = r.toByteArray()
    private val s: ByteArray = s.toByteArray()

    fun getRBase64(): String {
        return Base64.toBase64String(r)
    }

    fun getR(): BigInteger {
        return BigInteger(r)
    }

    fun getS(): BigInteger {
        return BigInteger(s)
    }
}
