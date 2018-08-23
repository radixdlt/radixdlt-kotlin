package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.PayloadAtom
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import io.reactivex.Completable
import java.util.HashMap

class DataStoreTranslator private constructor() {

    fun translate(dataStore: DataStore, atomBuilder: AtomBuilder): Completable {
        atomBuilder.type(PayloadAtom::class.java)
        atomBuilder.payload(dataStore.data.bytes)

        if (!dataStore.data.protectors.isEmpty()) {
            atomBuilder.protectors(dataStore.data.protectors)
        }

        if (dataStore.data.getMetaData().containsKey("application")) {
            atomBuilder.applicationId(dataStore.data.getMetaData()["application"] as String)
        }

        dataStore.getAddresses().forEach { atomBuilder.addDestination(it) }

        return Completable.complete()
    }

    fun fromAtom(atom: PayloadAtom): Data {
        val protectors: List<EncryptedPrivateKey> = if (atom.encryptor?.protectors != null) {
            atom.encryptor.protectors
        } else {
            emptyList()
        }

        val metaData = HashMap<String, Any?>()
        metaData["timestamp"] = atom.timestamp
        metaData["signatures"] = atom.signatures!!
        metaData["application"] = atom.applicationId
        metaData["encrypted"] = protectors.isNotEmpty()

        return Data.raw(atom.payload?.bytes, metaData, protectors)
    }

    companion object {
        val instance = DataStoreTranslator()
    }
}
