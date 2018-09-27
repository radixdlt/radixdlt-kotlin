package com.radixdlt.client.application.identity

import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECKeyPairGenerator
import org.bouncycastle.util.encoders.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.io.Writer
import java.security.GeneralSecurityException

/**
 * Radix Identity Helper methods
 */
object RadixIdentities {

    /**
     * Creates a radix identity from a private key
     * @param privateKeyBase64 the private key encoded in base 64
     * @return a radix identity
     */
    fun fromPrivateKeyBase64(privateKeyBase64: String): RadixIdentity {
        val myKey = ECKeyPair(Base64.decode(privateKeyBase64))
        return BaseRadixIdentity(myKey)
    }

    /**
     * Creates a new radix identity which is not stored anywhere
     * @return an unstored radix identity
     */
    fun createNew(): RadixIdentity {
        return BaseRadixIdentity(ECKeyPairGenerator.newInstance().generateKeyPair())
    }

    /**
     * Loads or creates an unencrypted file containing a private key and returns
     * the associated radix identity
     * @param keyFile the file to load or create
     * @return a radix identity
     * @throws IOException
     */
    @Throws(IOException::class)
    fun loadOrCreateFile(keyFile: File): RadixIdentity {
        val ecKeyPair: ECKeyPair
        if (keyFile.exists()) {
            ecKeyPair = ECKeyPair.fromFile(keyFile)
        } else {
            ecKeyPair = ECKeyPairGenerator.newInstance().generateKeyPair()
            FileOutputStream(keyFile).use { io ->
                io.write(ecKeyPair.getPrivateKey())
            }
        }

        return BaseRadixIdentity(ecKeyPair)
    }

    /**
     * Loads or creates an unencrypted file containing a private key and returns
     * the associated radix identity
     * @param filePath the path of the file to load or create
     * @return a radix identity
     * @throws IOException
     */
    @Throws(IOException::class)
    fun loadOrCreateFile(filePath: String): RadixIdentity {
        return loadOrCreateFile(File(filePath))
    }

    /**
     * Loads or creates an encrypted file containing a private key and returns
     * the associated radix identity
     * @param keyFile the file to load or create
     * @param password the password to decrypt the encrypted file
     * @return a radix identity
     * @throws IOException
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun loadOrCreateEncryptedFile(keyFile: File, password: String): RadixIdentity {
        return if (!keyFile.exists()) {
            FileWriter(keyFile).use { createNewEncryptedIdentity(it, password) }
        } else {
            FileReader(keyFile).use { readEncryptedIdentity(it, password) }
        }
    }

    /**
     * Loads or creates an encrypted file containing a private key and returns
     * the associated radix identity
     * @param filePath the path of the file to load or create
     * @param password the password to decrypt the encrypted file
     * @return a radix identity
     * @throws IOException
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun loadOrCreateEncryptedFile(filePath: String, password: String): RadixIdentity {
        return loadOrCreateEncryptedFile(File(filePath), password)
    }

    /**
     * Creates a new private key and encrypts it and then writes/flushes the result to a given writer
     *
     * @param writer the writer to write the encrypted private key to
     * @param password the password to encrypt the private key with
     * @return the radix identity created
     * @throws IOException
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun createNewEncryptedIdentity(writer: Writer, password: String): RadixIdentity {
        val encryptedKey = PrivateKeyEncrypter.createEncryptedPrivateKey(password)
        writer.write(encryptedKey)
        writer.flush()
        return readEncryptedIdentity(StringReader(encryptedKey), password)
    }

    /**
     * Reads an encrypted private key from a given reader and decrypts it with a given password
     * @param reader the reader to read the encrypted private key from
     * @param password the password to decrypt the private key with
     * @return the decrypted radix identity
     * @throws IOException
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    fun readEncryptedIdentity(reader: Reader, password: String): RadixIdentity {
        val key = ECKeyPair(PrivateKeyEncrypter.decryptPrivateKey(password, reader))
        return BaseRadixIdentity(key)
    }
}
