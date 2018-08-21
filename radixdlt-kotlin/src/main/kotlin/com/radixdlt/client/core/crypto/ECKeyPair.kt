package com.radixdlt.client.core.crypto

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.RadixHash
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.math.ec.ECPoint
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.security.KeyFactory
import java.util.Arrays

class ECKeyPair {
    @SerializedName("public")
    private val publicKey: ECPublicKey
    @Transient
    private val privateKey: ByteArray?

    constructor(publicKey: ECPublicKey) {
        this.publicKey = publicKey
        this.privateKey = null
    }

    constructor(publicKey: ByteArray, privateKey: ByteArray) {
        this.publicKey = ECPublicKey(publicKey)
        this.privateKey = privateKey.copyOf()
    }

    constructor(privateKey: ByteArray) {
        this.privateKey = privateKey.copyOf()

        val ecPrivateKey: ECPrivateKey

        try {
            val domain: ECDomainParameters = ECKeyPairGenerator.getDomain((this.privateKey.size - 1) * 8)!!
            val privateKeySpec: ECPrivateKeySpec = ECPrivateKeySpec(
                BigInteger(1, this.privateKey),
                ECParameterSpec(domain.getCurve(), domain.getG(), domain.getN(), domain.getH())
            )
            ecPrivateKey = KeyFactory.getInstance("EC", "BC").generatePrivate(privateKeySpec) as ECPrivateKey
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }

        try {
            val domain: ECDomainParameters = ECKeyPairGenerator.getDomain((this.privateKey.size - 1) * 8)!!
            val publicKeySpec: ECPublicKeySpec = ECPublicKeySpec(
                domain.getG().multiply(ecPrivateKey.getD()),
                ECParameterSpec(domain.getCurve(), domain.getG(), domain.getN(), domain.getH())
            )
            this.publicKey = ECPublicKey(
                (KeyFactory.getInstance(
                    "EC",
                    "BC"
                ).generatePublic(publicKeySpec) as org.bouncycastle.jce.interfaces.ECPublicKey).getQ().getEncoded(true)
            )
        } catch (e: Exception) {
            throw RuntimeException(e.message)
        }
    }

    fun getUID(): EUID {
        return publicKey.getUID()
    }

    fun encryptPrivateKey(publicKey: ECPublicKey): EncryptedPrivateKey {
        if (privateKey == null) {
            throw IllegalStateException("This key pair does not contain a private key.")
        }

        return EncryptedPrivateKey(publicKey.encrypt(privateKey))
    }

    fun getPublicKey(): ECPublicKey {
        return publicKey
    }

    fun getPrivateKey(): ByteArray {
        if (privateKey == null) {
            throw IllegalStateException("This key pair does not contain a private key.")
        }

        return Arrays.copyOf(privateKey, privateKey.size)
    }

    fun sign(data: ByteArray): ECSignature {
        val domain: ECDomainParameters = ECKeyPairGenerator.getDomain((getPublicKey().length() - 1) * 8)!!
        val signer: ECDSASigner = ECDSASigner()
        signer.init(true, ECPrivateKeyParameters(BigInteger(1, getPrivateKey()), domain))
        val components: Array<out BigInteger> = signer.generateSignature(data)
        return ECSignature(components[0], components[1])
    }

    @Throws(MacMismatchException::class)
    fun decrypt(data: ByteArray, sharedKey: EncryptedPrivateKey): ByteArray {
        if (privateKey == null) {
            throw IllegalStateException("This key does not contain a private key.")
        }

        val privateKey: ByteArray = decrypt(sharedKey.toByteArray())
        val sharedPrivateKey: ECKeyPair = ECKeyPair(privateKey)

        try {
            return sharedPrivateKey.decrypt(data)
        } catch (e: MacMismatchException) {
            println("${e.expectedBase64} ${e.actualBase64}")
            throw IllegalStateException("Unable to decrypt with shared private key.")
        }
    }

    @Throws(MacMismatchException::class)
    fun decrypt(data: ByteArray): ByteArray {
        if (privateKey == null) {
            throw IllegalStateException("This key does not contain a private key.")
        }

        try {
            val inputStream = DataInputStream(ByteArrayInputStream(data))

            // 1. Read the IV
            val iv = ByteArray(16)
            inputStream.readFully(iv)

            // 2. Read the ephemeral public key
            val publicKeySize: Int = inputStream.readUnsignedByte()
            val publicKeyRaw: ByteArray = ByteArray(publicKeySize)
            inputStream.readFully(publicKeyRaw)
            val ephemeral: ECPublicKey = ECPublicKey(publicKeyRaw)

            // 3. Do an EC point multiply with this.getPrivateKey() and ephemeral public key. This gives you a point M.
            val m: ECPoint = ephemeral.publicPoint.multiply(BigInteger(1, getPrivateKey())).normalize()

            // 4. Use the X component of point M and calculate the SHA512 hash H.
            val h: ByteArray = RadixHash.sha512of(m.xCoord.encoded).toByteArray()

            // 5. The first 32 bytes of H are called key_e and the last 32 bytes are called key_m.
            val keyE: ByteArray = Arrays.copyOfRange(h, 0, 32)
            val keyM: ByteArray = Arrays.copyOfRange(h, 32, 64)

            // 6. Read encrypted data
            val encrypted: ByteArray = ByteArray(inputStream.readInt())
            inputStream.readFully(encrypted)

            // 6. Read MAC
            val mac: ByteArray = ByteArray(32)
            inputStream.readFully(mac)

            // 7. Compare MAC with MAC'. If not equal, decryption will fail.
            val pkMac: ByteArray = publicKey.calculateMAC(keyM, iv, ephemeral, encrypted)
            if (!Arrays.equals(mac, pkMac)) {
                throw MacMismatchException(pkMac, mac)
            }

            // 8. Decrypt the cipher text with AES-256-CBC, using IV as initialization vector, key_e as decryption key
            //    and the cipher text as payload. The output is the padded input text.
            return publicKey.crypt(false, iv, encrypted, keyE)
        } catch (e: IOException) {
            // TODO: change type of exception thrown
            throw RuntimeException("Failed to decrypt", e)
        }
    }

    @Throws(MacMismatchException::class)
    fun decryptToAscii(data: ByteArray): String {
        return String(this.decrypt(data))
    }

    override fun hashCode(): Int {
        return this.publicKey.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ECKeyPair) {
            return false
        }

        return this.publicKey == other.getPublicKey()
    }

    // In Kotlin, accessing this method generates a new field in the DSON serializer
    // called Companion which has to be ignored.
    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun fromFile(file: File): ECKeyPair {
            BufferedInputStream(FileInputStream(file)).use { io ->
                val privateKey = ByteArray(32)
                val len = io.read(privateKey, 0, 32)

                if (len < 32) {
                    throw IllegalStateException("Private Key file must be 32 bytes")
                }

                return ECKeyPair(privateKey)
            }
        }
    }
}
