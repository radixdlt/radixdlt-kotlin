package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.DataParticle
import com.radixdlt.client.core.atoms.EncryptorParticle
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.crypto.Encryptor
import io.reactivex.Completable
import java.util.HashMap

class DataStoreTranslator private constructor() {

    // TODO: figure out correct method signature here (return Single<AtomBuilder> instead?)
    fun translate(dataStore: DataStore, atomBuilder: AtomBuilder): Completable {
        val payload = Payload(dataStore.data.bytes)
        val application = dataStore.data.getMetaData()["application"] as String?

        atomBuilder.setDataParticle(DataParticle(payload, application))
        val encryptor = dataStore.data.encryptor
        if (encryptor != null) {
            atomBuilder.setEncryptorParticle(EncryptorParticle(encryptor.protectors))
        }
        dataStore.getAddresses().forEach { atomBuilder.addDestination(it) }

        return Completable.complete()
    }

    // TODO: don't pass in maps, utilize a metadata builder?
    fun fromAtom(atom: Atom): Any {
        if (atom.dataParticle == null) {
            return Any()
        }

        val metaData = HashMap<String, Any?>()
        metaData["timestamp"] = atom.timestamp
        metaData["signatures"] = atom.signatures
        metaData.computeSynchronisedFunction("application") { _, _ -> atom.dataParticle.getMetaData("application") }
        metaData["encrypted"] = atom.encryptor != null

        val encryptor: Encryptor? = if (atom.encryptor != null) {
            Encryptor(atom.encryptor.protectors)
        } else {
            null
        }

        return Data.raw(atom.dataParticle.bytes?.bytes, metaData, encryptor)
    }

    companion object {
        @JvmStatic
        val instance = DataStoreTranslator()
    }
}

// Prevent null values
fun <K, V> HashMap<K, V>.computeSynchronisedFunction(key: K, remappingFunction: (t: K, u: V?) -> V) {
    return synchronized(this) {
        if (this[key] != null) {
            val valueSynchronized = remappingFunction(key, this[key])
            this[key] = valueSynchronized
        }
    }
}
