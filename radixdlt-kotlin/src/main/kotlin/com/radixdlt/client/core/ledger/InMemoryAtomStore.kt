package com.radixdlt.client.core.ledger

import com.radixdlt.client.application.translate.TransactionAtoms
import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
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
    private val cache = ConcurrentHashMap<RadixAddress, ReplaySubject<Atom>>()

    /**
     * Store an atom under a given destination
     * TODO: add synchronization if needed
     *
     * @param destination destination to store under
     * @param atom the atom to store
     */
    fun store(address: RadixAddress, atom: Atom) {
        cache.computeIfAbsentSynchronisedFunction(address) { ReplaySubject.create() }
            .onNext(atom)
    }

    /**
     * Returns an unending stream of validated atoms which are stored at a particular destination.
     *
     * @param address address (which determines shard) to query atoms for
     * @return an Atom Observable
     */
    override fun getAtoms(address: RadixAddress): Observable<Atom> {
        Objects.requireNonNull(address) // not needed in Kotlin
        return Observable.just(TransactionAtoms(address, Token.TEST.id))
            .flatMap { txAtoms ->
                cache.computeIfAbsentSynchronisedFunction(address) { euid -> ReplaySubject.create() }
                    .distinct()
                    .flatMap<Atom> { atom ->
                        if (!atom.getConsumables().isEmpty() && !atom.getConsumables().asSequence()
                                .all { c -> c is AtomFeeConsumable }) {
                            txAtoms.accept(atom).newValidTransactions
                        } else {
                            Observable.just(atom)
                        }
                    }
            }
    }
}
