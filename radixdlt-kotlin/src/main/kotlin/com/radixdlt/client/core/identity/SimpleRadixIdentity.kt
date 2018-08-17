package com.radixdlt.client.core.identity

import com.radixdlt.client.application.objects.EncryptedData
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.*
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SimpleRadixIdentity @Throws(IOException::class)
constructor(myKeyFile: File) : RadixIdentity {
    private val myKey: ECKeyPair

    init {
        if (myKeyFile.exists()) {
            myKey = ECKeyPair.fromFile(myKeyFile)
        } else {
            myKey = ECKeyPairGenerator.newInstance().generateKeyPair()
            FileOutputStream(myKeyFile).use { io -> io.write(myKey.getPrivateKey()) }
        }
    }

    @Throws(IOException::class)
    @JvmOverloads constructor(fileName: String = "my.key") : this(File(fileName))

    fun synchronousSign(atom: UnsignedAtom): Atom {
        val signature = myKey.sign(atom.hash.toByteArray())
        val signatureId = myKey.getUID()
        return atom.sign(signature, signatureId)
    }

    override fun sign(atom: UnsignedAtom): Single<Atom> {
        return Single.create { emitter ->
            val signature = myKey.sign(atom.hash.toByteArray())
            val signatureId = myKey.getUID()
            emitter.onSuccess(atom.sign(signature, signatureId))
        }
    }

    override fun decrypt(data: EncryptedData): Single<ByteArray> {
        for (protector in data.protectors) {
            // TODO: remove exception catching
            try {
                return Single.just(myKey.decrypt(data.encrypted, protector))
            } catch (e: MacMismatchException) {
            }

        }
        return Single.error(CryptoException("Cannot decrypt"))
    }

    override fun getPublicKey(): ECPublicKey {
        return myKey.getPublicKey()
    }
}
