package com.radixdlt.client.core.identity

import com.radixdlt.client.core.crypto.CryptoException
import io.reactivex.Single

object RadixIdentities {

    fun <T> decrypt(identity: RadixIdentity, decryptable: Decryptable<T>): Single<T> {
        return identity.decrypt(decryptable.getEncrypted())
                .map { decryptable.deserialize(it) }
                .onErrorResumeNext { throwable ->
                    Single.error(CryptoException("Unable to decrypt " + decryptable + " " + throwable.toString())
                    )
                }
    }
}
