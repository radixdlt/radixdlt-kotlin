package com.radixdlt.client.core.crypto

import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.security.Provider
import java.security.SecureRandomSpi
import java.security.Security

/**
 * Implementation from
 * [
 * BitcoinJ implementation](https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/crypto/LinuxSecureRandom.java)
 *
 *
 * A SecureRandom implementation that is able to override the standard JVM provided
 * implementation, and which simply serves random numbers by reading /dev/urandom. That is, it
 * delegates to the kernel on UNIX systems and is unusable on other platforms. Attempts to manually
 * set the seed are ignored. There is no difference between seed bytes and non-seed bytes, they are
 * all from the same source.
 */
class LinuxSecureRandom : SecureRandomSpi() {

    // DataInputStream is not thread safe, so each random object has its own.
    private val dis: DataInputStream = DataInputStream(URANDOM)

    private class LinuxSecureRandomProvider internal constructor() :
        Provider("LinuxSecureRandom", 1.0, "A Linux specific random number provider that uses /dev/urandom") {
        init {
            put("SecureRandom.LinuxSecureRandom", LinuxSecureRandom::class.java.name)
        }
    }

    override fun engineSetSeed(bytes: ByteArray) {
        // Ignore.
    }

    override fun engineNextBytes(bytes: ByteArray) {
        try {
            dis.readFully(bytes) // This will block until all the bytes can be read.
        } catch (e: IOException) {
            throw RuntimeException(e) // Fatal error. Do not attempt to recover from this.
        }
    }

    override fun engineGenerateSeed(i: Int): ByteArray {
        val bits = ByteArray(i)
        engineNextBytes(bits)
        return bits
    }

    companion object {
        private val URANDOM: FileInputStream

        init {
            try {
                val file = File("/dev/urandom")
                // This stream is deliberately leaked.
                URANDOM = FileInputStream(file)
                if (URANDOM.read() == -1) {
                    throw RuntimeException("/dev/urandom not readable?")
                }
                // Now override the default SecureRandom implementation with this one.
                val position = Security.insertProviderAt(LinuxSecureRandomProvider(), 1)

                if (position != -1) {
                    println("Secure randomness will be read from {} only. " + file.path)
                } else {
                    println("Randomness is already secure.")
                }
            } catch (e: FileNotFoundException) {
                // Should never happen.
                throw RuntimeException("/dev/urandom does not appear to exist or is not openable", e)
            } catch (e: IOException) {
                throw RuntimeException("/dev/urandom does not appear to be readable", e)
            }
        }
    }
}
