package com.radixdlt.client.core.ledger

import com.radixdlt.client.application.translate.TransactionAtoms
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AtomObservation
import com.radixdlt.client.core.atoms.Consumable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import java.util.concurrent.ConcurrentHashMap

class ConsumableDataSource(private val atomStore: (EUID) -> (Observable<AtomObservation>)) : ParticleStore {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Collection<Consumable>>>()

    override fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
        // TODO: use https://github.com/JakeWharton/RxReplayingShare to disconnect when unsubscribed
        return cache.computeIfAbsentSynchronisedFunction(address) { _ ->
            Observables.combineLatest(
                Observable.fromCallable { TransactionAtoms(address, Asset.TEST.id) },
                atomStore(address.getUID())
                    .filter { o -> o.isHead || o.atom!!.isTransactionAtom }
            ) { transactionAtoms, atomObservation ->
                if (atomObservation.isHead) {
                    return@combineLatest Maybe.just(transactionAtoms.getUnconsumedConsumables())
                } else {
                    val atom = atomObservation.atom!!.asTransactionAtom
                    transactionAtoms.accept(atom)
                    return@combineLatest Maybe.empty<Collection<Consumable>>()
                }
            }.flatMapMaybe { unconsumedMaybe -> unconsumedMaybe }
                .replay(1).autoConnect()
        }
    }
}

/**
 * Implementation that doesn't block when map already
 * contains the value
 */
fun <K, V> ConcurrentHashMap<K, V>.computeIfAbsentFunction(key: K, mappingFunction: (t: K) -> V): V {
    return this[key] ?: run {
        val value = mappingFunction(key)
        this[key] = value
        return@run value
    }
}

/**
 * Synchronised that doesn't block when map already
 * contains the value
 */
fun <K, V> ConcurrentHashMap<K, V>.computeIfAbsentSynchronisedFunction(key: K, mappingFunction: (t: K) -> V): V {
    return this[key] ?: synchronized(this) {
        var valueSynchronized = get(key)
        if (valueSynchronized == null) {
            valueSynchronized = mappingFunction(key)
            this[key] = valueSynchronized
        }
        return@synchronized valueSynchronized!!
    }
}
