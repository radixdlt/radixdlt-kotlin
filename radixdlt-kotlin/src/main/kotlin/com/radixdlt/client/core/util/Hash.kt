package com.radixdlt.client.core.util

import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.Security

object Hash {

    init {
        if (AndroidUtil.isAndroidRuntime) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        }
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    private fun hash(algorithm: String, data: ByteArray, offset: Int, len: Int): ByteArray {
        try {
            val messageDigest = MessageDigest.getInstance(algorithm, "BC")
            synchronized(messageDigest) {
                messageDigest.update(data, offset, len)
                return messageDigest.digest()
            }
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e.message)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException(e.message)
        }
    }

    @JvmStatic
    fun sha512(data: ByteArray): ByteArray {
        return hash("SHA-512", data, 0, data.size)
    }

    // Hashes the specified byte array using SHA-256
    @JvmStatic
    @JvmOverloads
    fun sha256(data: ByteArray, offset: Int = 0, len: Int = data.size): ByteArray {
        return hash("SHA-256", data, offset, len)
    }
}