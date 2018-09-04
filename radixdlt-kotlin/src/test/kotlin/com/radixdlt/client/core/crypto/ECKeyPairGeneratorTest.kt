package com.radixdlt.client.core.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.security.Provider
import java.security.SecureRandomSpi
import java.security.Security
import java.util.concurrent.atomic.AtomicInteger

class ECKeyPairGeneratorTest {
    class FakeSha1Provider internal constructor() : Provider("FakeSha1Provider", 1.0, null) {
        init {
            put("SecureRandom.SHA1PRNG", FakeAlgorithm::class.java.name)
        }
    }

    class FakeAlgorithm : SecureRandomSpi() {
        private val cur = AtomicInteger(0)

        override fun engineSetSeed(seed: ByteArray) {
            for (i in seed.indices) {
                // Deterministic result for a given length
                seed[i] = cur.getAndIncrement().toByte()
            }
        }

        override fun engineNextBytes(bytes: ByteArray) {
            for (i in bytes.indices) {
                bytes[i] = cur.getAndIncrement().toByte()
            }
        }

        override fun engineGenerateSeed(numBytes: Int): ByteArray {
            val bytes = ByteArray(numBytes)
            for (i in 0 until numBytes) {
                bytes[i] = cur.getAndIncrement().toByte()
            }
            return bytes
        }
    }

    @Test
    fun generateKeyPair() {
        assertNotNull(ECKeyPairGenerator.newInstance().generateKeyPair())
    }

    @Test
    fun test256bits() {
        for (i in 0..999) {
            val ecKeyPair = ECKeyPairGenerator.newInstance().generateKeyPair()
            assertEquals(32, ecKeyPair.getPrivateKey().size.toLong())
        }
    }

    @Test
    @Throws(MacMismatchException::class)
    fun encryptionTest() {
        val testPhrase = "Hello World"
        val ecKeyPair = ECKeyPairGenerator.newInstance().generateKeyPair()
        val encrypted = ecKeyPair.getPublicKey().encrypt(testPhrase.toByteArray())
        assertTrue(encrypted.size > 0)
        val decrypted = ecKeyPair.decrypt(encrypted)
        assertEquals(testPhrase, String(decrypted))
    }

    companion object {

        @BeforeClass
        fun init() {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            // Give fake provider highest priority
            Security.insertProviderAt(FakeSha1Provider(), 1)
            Security.insertProviderAt(BouncyCastleProvider(), 2)
        }

        @AfterClass
        fun after() {
            Security.removeProvider("FakeSha1Provider")
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }
}
