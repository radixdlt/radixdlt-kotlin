package com.radixdlt.client.application.identity

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.radixdlt.client.application.identity.model.Cipherparams
import com.radixdlt.client.application.identity.model.Crypto
import com.radixdlt.client.application.identity.model.Keystore
import com.radixdlt.client.application.identity.model.Pbkdfparams
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECKeyPairGenerator
import com.radixdlt.client.core.crypto.LinuxSecureRandom
import com.radixdlt.client.core.crypto.MacMismatchException
import com.radixdlt.client.core.util.AndroidUtil
import okio.ByteString
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.Security
import java.security.spec.InvalidKeySpecException
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PrivateKeyEncrypter {

    private val secureRandom: SecureRandom

    private const val ITERATIONS = 100000
    private const val KEY_LENGTH = 32
    private const val DIGEST = "sha512"
    private const val ALGORITHM = "aes-256-ctr"

    private val salt: String
        get() {
            val salt = ByteArray(32)
            secureRandom.nextBytes(salt)
            return ByteString.of(*salt).hex()
        }

    // In order to prevent the possibility of developers who may use or tweak the library for versions of Android
    // lower than API 17 Jellybean where there is a vulnerability in the implementation of SecureRandom, the below
    // initialisation of SecureRandom on Android fixes the potential issue.
    init {
        if (AndroidUtil.isAndroidRuntime) {
            LinuxSecureRandom()
            // Ensure the library version of BouncyCastle is used for Android
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
        secureRandom = SecureRandom()
    }

    @Throws(GeneralSecurityException::class)
    fun createEncryptedPrivateKey(password: String): String {
        val ecKeyPair = ECKeyPairGenerator.newInstance().generateKeyPair()
        val privateKey = ByteString.of(*ecKeyPair.getPrivateKey()).hex()
        val salt = salt.toByteArray(StandardCharsets.UTF_8)

        val derivedKey = getSecretKey(password, salt, ITERATIONS, KEY_LENGTH)

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey)
        val iv = cipher.parameters.getParameterSpec(IvParameterSpec::class.java).iv

        val cipherText = encrypt(cipher, privateKey)
        val mac = generateMac(derivedKey.encoded, ByteString.decodeHex(cipherText).toByteArray())

        val keystore = createKeystore(ecKeyPair, cipherText, mac, iv, salt)

        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(keystore)
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun decryptPrivateKey(password: String, keyReader: Reader): ByteArray {
        val keystore = getKeystore(keyReader)
        val salt = keystore.crypto!!.pbkdfparams!!.salt.toByteArray()
        val iterations = keystore.crypto!!.pbkdfparams!!.iterations
        val keyLen = keystore.crypto!!.pbkdfparams!!.keylen
        val iv = ByteString.decodeHex(keystore.crypto?.cipherparams!!.iv).toByteArray()
        val mac = ByteString.decodeHex(keystore.crypto!!.mac).toByteArray()
        val cipherText = ByteString.decodeHex(keystore.crypto!!.ciphertext).toByteArray()

        val derivedKey = getSecretKey(password, salt, iterations, keyLen)

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derivedKey, IvParameterSpec(iv))

        val computedMac = generateMac(derivedKey.encoded, cipherText)

        if (!Arrays.equals(computedMac, mac)) {
            throw MacMismatchException(computedMac, mac)
        }

        val privateKey = decrypt(cipher, cipherText)

        return ByteString.decodeHex(privateKey).toByteArray()
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getSecretKey(passPhrase: String, salt: ByteArray, iterations: Int, keyLength: Int): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val spec = PBEKeySpec(passPhrase.toCharArray(), salt, iterations, keyLength * 8)
        val key = factory.generateSecret(spec)

        return SecretKeySpec(key.encoded, "AES")
    }

    @Throws(IOException::class)
    private fun getKeystore(keyReader: Reader): Keystore {
        JsonReader(keyReader).use { jsonReader ->
            return Gson().fromJson(jsonReader, Keystore::class.java)
        }
    }

    private fun createKeystore(
        ecKeyPair: ECKeyPair,
        cipherText: String,
        mac: ByteArray,
        iv: ByteArray,
        salt: ByteArray
    ): Keystore {

        val pbkdfparams = Pbkdfparams().apply {
            digest = DIGEST
            iterations = ITERATIONS
            keylen = KEY_LENGTH
            this.salt = String(salt, StandardCharsets.UTF_8)
        }

        val cipherparams = Cipherparams().apply {
            this.iv = ByteString.of(*iv).hex()
        }

        val crypto = Crypto().apply {
            cipher = ALGORITHM
            ciphertext = cipherText
            this.mac = ByteString.of(*mac).hex()
            this.cipherparams = cipherparams
            this.pbkdfparams = pbkdfparams
        }

        return Keystore().apply {
            id = ecKeyPair.getUID().toString()
            this.crypto = crypto
        }
    }

    @Throws(GeneralSecurityException::class)
    private fun encrypt(cipher: Cipher, encrypt: String): String {
        val bytes = encrypt.toByteArray(StandardCharsets.UTF_8)
        val encrypted = cipher.doFinal(bytes)
        return ByteString.of(*encrypted).hex()
    }

    @Throws(GeneralSecurityException::class)
    private fun decrypt(cipher: Cipher, encrypted: ByteArray): String {
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun generateMac(derivedKey: ByteArray, cipherText: ByteArray): ByteArray {
        var result = ByteArray(derivedKey.size + cipherText.size)
        result = ByteBuffer.wrap(result).put(derivedKey).put(cipherText).array()

        return RadixHash.of(result).toByteArray()
    }
}
