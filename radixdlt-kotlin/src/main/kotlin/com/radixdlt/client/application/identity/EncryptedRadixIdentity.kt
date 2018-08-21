package com.radixdlt.client.application.identity

import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.UnencryptedData
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.MacMismatchException
import io.reactivex.Single
import java.io.File

class EncryptedRadixIdentity @Throws(Exception::class)
constructor(password: String, myKeyFile: File) : RadixIdentity {
    private val myKey: ECKeyPair

    init {
        myKey = if (myKeyFile.exists()) {
            getECKeyPair(password, myKeyFile)
        } else {
            PrivateKeyEncrypter.createEncryptedPrivateKeyFile(password, myKeyFile.path)
            getECKeyPair(password, myKeyFile)
        }
    }

    @Throws(Exception::class)
    @JvmOverloads constructor(password: String, fileName: String = "my_encrypted.key") : this(password, File(fileName))

    @Throws(Exception::class)
    private fun getECKeyPair(password: String, myKeyfile: File): ECKeyPair {
        return ECKeyPair(PrivateKeyEncrypter.decryptPrivateKeyFile(password, myKeyfile.path))
    }

    override fun sign(atom: UnsignedAtom): Single<Atom> {
        return Single.create { emitter ->
            val signature = myKey.sign(atom.hash.toByteArray())
            val signatureId = myKey.getUID()
            emitter.onSuccess(atom.sign(signature, signatureId))
        }
    }

    override fun decrypt(data: Data): Single<UnencryptedData> {
        val encrypted = data.getMetaData()["encrypted"] as Boolean
        if (encrypted) {
            for (protector in data.protectors) {
                // TODO: remove exception catching
                try {
                    val bytes = myKey.decrypt(data.bytes!!, protector)
                    return Single.just(UnencryptedData(bytes, data.getMetaData(), true))
                } catch (e: MacMismatchException) {
                }
            }
            return Single.error(CryptoException("Cannot decrypt"))
        } else {
            return Single.just(UnencryptedData(data.bytes!!, data.getMetaData(), false))
        }
    }

    override fun getPublicKey(): ECPublicKey {
        return myKey.getPublicKey()
    }
}
