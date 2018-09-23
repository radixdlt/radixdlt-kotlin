package com.radixdlt.client.core.ledger

import com.radixdlt.client.application.translate.computeIfAbsentSynchronisedFunction
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.ConcurrentHashMap

class AtomPuller(private val fetcher: (EUID) -> (Observable<Atom>), private val atomStore: (EUID, Atom) -> Unit) {

    /**
     * Atoms retrieved from the network
     */
    private val cache = ConcurrentHashMap<EUID, Observable<Atom>>()

    fun pull(euid: EUID): Disposable {
        return cache.computeIfAbsentSynchronisedFunction(
            euid
        ) { destination ->
            val fetchedAtoms = fetcher(destination)
                .publish().refCount(2)
            fetchedAtoms.subscribe { atom -> atomStore(euid, atom) }
            fetchedAtoms
        }.subscribe()
    }
}
