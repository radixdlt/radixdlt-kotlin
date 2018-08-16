package com.radixdlt.client.core.identity

import com.radixdlt.client.application.EncryptedData
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.ECPublicKey
import io.reactivex.Single


interface RadixIdentity {
    fun getPublicKey(): ECPublicKey
    fun sign(atom: UnsignedAtom): Single<Atom>
    fun decrypt(data: EncryptedData): Single<ByteArray>
}
