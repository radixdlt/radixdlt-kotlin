package com.radixdlt.client.core.identity

import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.EncryptedPayload
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECKeyPairGenerator
import com.radixdlt.client.core.crypto.ECPublicKey
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

    override fun decrypt(data: EncryptedPayload?): Single<ByteArray> {
        return Single.fromCallable { data?.decrypt(myKey) }
    }

    override fun getPublicKey(): ECPublicKey {
        return myKey.getPublicKey()
    }
}
