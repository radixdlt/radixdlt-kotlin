package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AtomObservation
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.ConcurrentHashMap

/**
 * Module responsible for fetches and merges of new atoms into the Atom Store.
 */
class RadixAtomPuller(
    /**
     * The mechanism by which to fetch atoms
     */
    private val fetcher: (EUID) -> (Observable<AtomObservation>),
    /**
     * The mechanism by which to merge or store atoms
     */
    private val atomStore: (EUID, AtomObservation) -> Unit
) : AtomPuller {

    /**
     * Atoms retrieved from the network
     */
    private val cache = ConcurrentHashMap<EUID, Observable<AtomObservation>>()

    override fun pull(euid: EUID): Disposable {
        return cache.computeIfAbsentSynchronisedFunction(
            euid
        ) { destination ->
            val fetchedAtoms = fetcher(destination)
                .publish().refCount(2)
            fetchedAtoms.subscribe { atomObservation -> atomStore(euid, atomObservation) }
            fetchedAtoms
        }.subscribe()
    }
}
