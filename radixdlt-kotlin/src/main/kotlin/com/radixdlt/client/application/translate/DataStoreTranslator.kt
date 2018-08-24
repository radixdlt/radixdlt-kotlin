package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.DataParticle
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import io.reactivex.Completable
import java.util.HashMap

class DataStoreTranslator private constructor() {

    fun translate(dataStore: DataStore, atomBuilder: AtomBuilder): Completable {
        val payload = Payload(dataStore.data.bytes)
        val application = dataStore.data.getMetaData()["application"] as String?

        atomBuilder.setDataParticle(DataParticle(payload, application))
        dataStore.getAddresses().forEach { atomBuilder.addDestination(it) }

        return Completable.complete()
    }

    fun fromAtom(atom: Atom): Any {
        if (atom.dataParticle == null) {
            return Any()
        }
        val protectors: List<EncryptedPrivateKey> = if (atom.encryptor?.protectors != null) {
            atom.encryptor.protectors
        } else {
            emptyList()
        }

        val metaData = HashMap<String, Any?>()
        metaData["timestamp"] = atom.timestamp
        metaData["signatures"] = atom.signatures
        metaData["application"] = atom.dataParticle.application
        metaData["encrypted"] = protectors.isNotEmpty()

        return Data.raw(atom.dataParticle.bytes.bytes, metaData, protectors)
    }

    companion object {
        val instance = DataStoreTranslator()
    }
}
