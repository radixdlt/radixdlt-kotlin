package com.radixdlt.client.core.identity

import com.radixdlt.client.application.EncryptedData
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.*
import io.reactivex.Single

// Simply generate a key pair and don't worry about saving it
class OneTimeUseIdentity : RadixIdentity {
    private val myKey: ECKeyPair = ECKeyPairGenerator.newInstance().generateKeyPair()

    fun synchronousSign(unsignedAtom: UnsignedAtom): Atom {
        val signature = myKey.sign(unsignedAtom.hash.toByteArray())
        val signatureId = myKey.getUID()
        return unsignedAtom.sign(signature, signatureId)
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
