package com.radixdlt.client.core.ledger

import com.radixdlt.client.application.translate.TransactionAtoms
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.Consumable
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ConsumableDataSource(private val atomStore: (EUID) -> (Observable<Atom>)) : ParticleStore {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Collection<Consumable>>>()

    override fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
        // TODO: use https://github.com/JakeWharton/RxReplayingShare to disconnect when unsubscribed
        return cache.computeIfAbsentSynchronisedFunction(address) { _ ->
            Observable.just<Collection<Consumable>>(emptySet()).concatWith(
                Observables.combineLatest(
                    Observable.fromCallable {
                        TransactionAtoms(
                            address,
                            Asset.TEST.id
                        )
                    },
                    atomStore(address.getUID())
                        .filter(Atom::isTransactionAtom)
                        .map(Atom::asTransactionAtom)
                ) { transactionAtoms, atom ->
                    transactionAtoms.accept(atom)
                        .getUnconsumedConsumables()
                }.flatMapMaybe({ unconsumedMaybe -> unconsumedMaybe })
            ).debounce(1000, TimeUnit.MILLISECONDS)
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