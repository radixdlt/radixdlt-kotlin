package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.StoreDataAction
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import io.reactivex.Completable
import java.util.HashMap

class DataStoreTranslator private constructor() {

    fun translate(storeDataAction: StoreDataAction, atomBuilder: AtomBuilder): Completable {
        atomBuilder.type(ApplicationPayloadAtom::class.java)
        atomBuilder.payload(storeDataAction.data.bytes)

        if (!storeDataAction.data.protectors.isEmpty()) {
            atomBuilder.protectors(storeDataAction.data.protectors)
        }

        if (storeDataAction.data.getMetaData().containsKey("application")) {
            atomBuilder.applicationId(storeDataAction.data.getMetaData()["application"] as String)
        }

        storeDataAction.getAddresses().forEach { atomBuilder.addDestination(it) }

        return Completable.complete()
    }

    fun fromAtom(atom: ApplicationPayloadAtom): Data {
        val protectors: List<EncryptedPrivateKey> = if (atom.encryptor?.protectors != null) {
            atom.encryptor.protectors
        } else {
            emptyList()
        }

        val metaData = HashMap<String, Any>()
        metaData["timestamp"] = atom.timestamp
        metaData["signatures"] = atom.signatures!!
        metaData["application"] = atom.applicationId
        metaData["encrypted"] = protectors.isNotEmpty()

        return Data.raw(atom.encrypted?.bytes, metaData, protectors)
    }

    companion object {
        @JvmStatic
        val instance = DataStoreTranslator()
    }
}
