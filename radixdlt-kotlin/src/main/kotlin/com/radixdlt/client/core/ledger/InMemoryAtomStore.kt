package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AtomObservation
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of a data store for all atoms in a shard
 */
class InMemoryAtomStore : AtomStore {

    /**
     * The In Memory Atom Data Store
     */
    private val cache = ConcurrentHashMap<EUID, ReplaySubject<AtomObservation>>()

    /**
     * Store an atom under a given destination
     * TODO: add synchronization if needed
     *
     * @param destination destination to store under
     * @param atomObservation the atom to store
     */
    fun store(destination: EUID, atomObservation: AtomObservation) {
        cache.computeIfAbsentSynchronisedFunction(destination) { ReplaySubject.create() }
            .onNext(atomObservation)
    }

    /**
     * Returns an unending stream of atoms which are stored at a particular destination.
     *
     * @param destination destination (which determines shard) to query atoms for
     * @return an Atom Observable
     */
    override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
        Objects.requireNonNull(destination!!)
        return cache.computeIfAbsentSynchronisedFunction(destination) { ReplaySubject.create() }
            .distinct()
    }
}
