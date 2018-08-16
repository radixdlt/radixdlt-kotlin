package com.radixdlt.client.core.util

interface Base64Encoded {
    fun base64(): String
    fun toByteArray(): ByteArray
}
