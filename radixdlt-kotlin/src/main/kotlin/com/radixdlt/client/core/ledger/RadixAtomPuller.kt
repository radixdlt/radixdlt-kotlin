package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
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
    private val fetcher: (RadixAddress) -> (Observable<Atom>),
    /**
     * The mechanism by which to merge or store atoms
     */
    private val atomStore: (RadixAddress, Atom) -> Unit
) : AtomPuller {

    /**
     * Atoms retrieved from the network
     */
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Atom>>()

    override fun pull(address: RadixAddress): Disposable {
        return cache.computeIfAbsentSynchronisedFunction(address) { destination ->
            val fetchedAtoms = fetcher(destination)
                .publish().refCount(2)
            fetchedAtoms.subscribe { atom -> atomStore(address, atom) }
            fetchedAtoms
        }.subscribe()
    }
}
