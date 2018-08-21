package com.radixdlt.client.core.crypto

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.util.Base64Encoded
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ECPublicKey(publicKey: ByteArray) : Base64Encoded {
    private val publicKey: ByteArray = Arrays.copyOf(publicKey, publicKey.size)

    val publicPoint: ECPoint
        get() {
            val domainSize =
                if (this.publicKey[0].toInt() == 4) (this.publicKey.size / 2 - 1) * 8 else (this.publicKey.size - 1) * 8

            val domain = ECKeyPairGenerator.getDomain(domainSize)
                ?: throw RuntimeException("Invalid domain key size " + (this.publicKey.size - 1) * 8)

            return domain.curve.decodePoint(this.publicKey)
        }

    fun copyPublicKey(dest: ByteArray, destPos: Int) {
        System.arraycopy(publicKey, 0, dest, destPos, publicKey.size)
    }

    fun length(): Int {
        return publicKey.size
    }

    override fun base64(): String {
        return Base64.toBase64String(publicKey)
    }

    override fun toByteArray(): ByteArray {
        return Arrays.copyOf(publicKey, publicKey.size)
    }

    fun getUID(): EUID {
        return RadixHash.of(publicKey).toEUID()
    }

    fun toECKeyPair(): ECKeyPair {
        return ECKeyPair(this)
    }

    fun verify(data: ByteArray, signature: ECSignature): Boolean {
        val domain = ECKeyPairGenerator.getDomain((this.length() - 1) * 8)

        val verifier = ECDSASigner()
        verifier.init(false, ECPublicKeyParameters(domain!!.curve.decodePoint(publicKey), domain))

        return verifier.verifySignature(data, signature.getR(), signature.getS())
    }

    override fun hashCode(): Int {
        // Slow but works for now
        return base64().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        // Slow but works for now
        return if (other == null || other !is ECPublicKey) false else other.base64() == this.base64()
    }

    override fun toString(): String {
        return base64()
    }

    @Throws(IOException::class)
    fun calculateMAC(salt: ByteArray, iv: ByteArray, ephemeralPublicKey: ECPublicKey, encrypted: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        val outputStream = DataOutputStream(baos)

        outputStream.write(iv)
        // outputStream.writeByte(ephemeralPublicKey.length());
        outputStream.write(ephemeralPublicKey.publicKey)
        // outputStream.writeInt(encrypted.length);
        outputStream.write(encrypted)

        try {
            val mac = Mac.getInstance("HmacSHA256", "BC")
            mac.init(SecretKeySpec(salt, "HmacSHA256"))
            return mac.doFinal(baos.toByteArray())
        } catch (e: GeneralSecurityException) {
            throw IOException(e)
        }
    }

    fun crypt(encrypt: Boolean, iv: ByteArray, data: ByteArray, keyE: ByteArray): ByteArray {
        try {
            val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()), PKCS7Padding())

            val params = ParametersWithIV(KeyParameter(keyE), iv)

            cipher.init(encrypt, params)

            val buffer = ByteArray(cipher.getOutputSize(data.size))

            var length = cipher.processBytes(data, 0, data.size, buffer, 0)
            length += cipher.doFinal(buffer, length)

            return if (length < buffer.size) {
                Arrays.copyOfRange(buffer, 0, length)
            } else buffer
        } catch (e: InvalidCipherTextException) {
            throw RuntimeException(e)
        }
    }

    fun encrypt(data: ByteArray): ByteArray {
        try {
            val rand = SecureRandom()

            // 1. The destination is this.getPublicKey()
            // 2. Generate 16 random bytes using a secure random number generator. Call them IV
            val iv = ByteArray(16)
            rand.nextBytes(iv)

            // 3. Generate a new ephemeral EC key pair
            val ephemeral = ECKeyPairGenerator.newInstance().generateKeyPair((publicKey.size - 1) * 8)

            // 4. Do an EC point multiply with this.getPublicKey() and ephemeral private key. This gives you a point M.
            val m = publicPoint.multiply(BigInteger(1, ephemeral.getPrivateKey())).normalize()

            // 5. Use the X component of point M and calculate the SHA512 hash H.
            val h = RadixHash.sha512of(m.xCoord.encoded).toByteArray()

            // 6. The first 32 bytes of H are called key_e and the last 32 bytes are called key_m.
            val keyE = Arrays.copyOfRange(h, 0, 32)
            val keyM = Arrays.copyOfRange(h, 32, 64)

            // 7. Pad the input text to a multiple of 16 bytes, in accordance to PKCS7.
            // 8. Encrypt the data with AES-256-CBC, using IV as initialization vector,
            // key_e as encryption key and the padded input text as encrypted. Call the output cipher text.
            val encrypted = crypt(true, iv, data, keyE)

            // 9. Calculate a 32 byte MAC with HMACSHA256, using key_m as salt and
            // IV + ephemeral.pub + cipher text as data. Call the output MAC.
            val mac = calculateMAC(keyM, iv, ephemeral.getPublicKey(), encrypted)

            // 10. Write out the encryption result IV + ephemeral.pub + encrypted + MAC
            val baos = ByteArrayOutputStream()
            val outputStream = DataOutputStream(baos)
            outputStream.write(iv)
            outputStream.writeByte(ephemeral.getPublicKey().length())
            outputStream.write(ephemeral.getPublicKey().publicKey)
            outputStream.writeInt(encrypted.size)
            outputStream.write(encrypted)
            outputStream.write(mac)

            return baos.toByteArray()
        } catch (ioex: IOException) {
            throw RuntimeException(ioex)
        }
    }
}
