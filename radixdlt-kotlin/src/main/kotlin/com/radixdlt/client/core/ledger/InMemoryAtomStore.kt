package com.radixdlt.client.core.ledger

import com.radixdlt.client.application.translate.computeIfAbsentSynchronisedFunction
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of a data store for all atoms in a shard
 */
class InMemoryAtomStore {

    /**
     * The In Memory Atom Data Store
     */
    private val cache = ConcurrentHashMap<EUID, ReplaySubject<Atom>>()

    /**
     * Store an atom under a given destination
     * TODO: add synchronization if needed
     *
     * @param destination destination to store under
     * @param atom the atom to store
     */
    fun store(destination: EUID, atom: Atom) {
        cache.computeIfAbsentSynchronisedFunction(destination) { euid -> ReplaySubject.create() }
            .onNext(atom)
    }

    /**
     * Returns an unending stream of atoms which are stored at a particular destination.
     *
     * @param destination destination (which determines shard) to query atoms for
     * @return an Atom Observable
     */
    fun getAtoms(destination: EUID?): Observable<Atom> {
        Objects.requireNonNull(destination!!)
        return cache.computeIfAbsentSynchronisedFunction(destination) { euid -> ReplaySubject.create() }
            .distinct()
    }
}
