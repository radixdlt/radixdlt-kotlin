package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
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
    private val fetcher: (EUID) -> (Observable<Atom>),
    /**
     * The mechanism by which to merge or store atoms
     */
    private val atomStore: (EUID, Atom) -> Unit) : AtomPuller {

    /**
     * Atoms retrieved from the network
     */
    private val cache = ConcurrentHashMap<EUID, Observable<Atom>>()

    override fun pull(euid: EUID): Disposable {
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
