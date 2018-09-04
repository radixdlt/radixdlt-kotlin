package com.radixdlt.client.application.translate

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.ledger.RadixLedger
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Observables
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ConsumableDataSource(private val ledger: RadixLedger) {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Collection<Consumable>>>()

    fun getCurrentConsumables(address: RadixAddress): Single<Collection<Consumable>> {
        return this.getConsumables(address).firstOrError()
    }

    fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
        // TODO: use https://github.com/JakeWharton/RxReplayingShare to disconnect when unsubscribed
        return cache.computeIfAbsentSynchronisedFunction(address) { _ ->
            Observable.just<Collection<Consumable>>(emptySet()).concatWith(
                Observables.combineLatest(
                    Observable.fromCallable { TransactionAtoms(address, Asset.TEST.id) },
                    ledger.getAllAtoms(address.getUID())
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
