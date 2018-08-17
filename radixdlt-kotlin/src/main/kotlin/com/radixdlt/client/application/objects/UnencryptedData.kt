package com.radixdlt.client.application.objects

import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.identity.RadixIdentity
import io.reactivex.Maybe
import java.util.HashMap

class UnencryptedData(val data: ByteArray, val metaData: Map<String, Any>) {
    companion object {

        @JvmStatic
        fun fromAtom(atom: ApplicationPayloadAtom, identity: RadixIdentity): Maybe<UnencryptedData> {
            return if (atom.encryptor?.protectors != null) {
                val encryptedData = EncryptedData.fromAtom(atom)
                identity.decrypt(encryptedData)
                        .map { data -> UnencryptedData(data, encryptedData.metaData) }
                        .toMaybe().onErrorComplete()
            } else {
                val metaData = HashMap<String, Any>()
                metaData["timestamp"] = atom.timestamp
                metaData["signatures"] = atom.signatures!!
                metaData["application"] = atom.applicationId
                Maybe.just(UnencryptedData(atom.encrypted!!.bytes, metaData))
            }
        }
    }
}
