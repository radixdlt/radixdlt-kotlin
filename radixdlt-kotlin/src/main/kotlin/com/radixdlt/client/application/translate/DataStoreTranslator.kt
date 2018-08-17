package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.AtomBuilder
import io.reactivex.Completable

class DataStoreTranslator private constructor() {

    fun translate(dataStore: DataStore, atomBuilder: AtomBuilder): Completable {
        atomBuilder
                .type(ApplicationPayloadAtom::class.java)
                .protectors(dataStore.protectors)
                .payload(dataStore.data)

        if (dataStore.metaData.containsKey("application")) {
            atomBuilder.applicationId(dataStore.metaData["application"] as String)
        }

        dataStore.addresses.forEach { atomBuilder.addDestination(it) }

        return Completable.complete()
    }

    companion object {
        val instance = DataStoreTranslator()
    }
}
