package com.radixdlt.client.application.identity

import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.UnencryptedData
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.ECPublicKey
import io.reactivex.Single

interface RadixIdentity {
    fun sign(atom: UnsignedAtom): Single<Atom>

    /**
     * Transforms a possibly encrypted bytes object into an unencrypted one.
     * If decryption fails then return an empty Maybe.
     * @param data bytes to transform
     * @return either the unencrypted version of the bytes or an error
     */
    fun decrypt(data: Data): Single<UnencryptedData>

    fun getPublicKey(): ECPublicKey
}
