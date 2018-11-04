package com.radixdlt.client.core.crypto

import org.bouncycastle.util.encoders.Base64

class MacMismatchException : CryptoException {
    private val expected: ByteArray
    private val actual: ByteArray

    val expectedBase64: String
        get() = Base64.toBase64String(expected)

    val actualBase64: String
        get() = Base64.toBase64String(actual)

    constructor(msg: String, expected: ByteArray, actual: ByteArray) : super(msg) {
        this.expected = expected
        this.actual = actual
    }

    constructor(expected: ByteArray, actual: ByteArray) {
        this.expected = expected
        this.actual = actual
    }
}
