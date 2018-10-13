package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.particles.Consumable
import io.reactivex.Observable
import java.util.concurrent.ConcurrentHashMap

class ConsumableDataSource(private val atomStore: AtomStore) : ParticleStore {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Consumable>>()

    override fun getConsumables(address: RadixAddress): Observable<Consumable> {
        // TODO: use https://github.com/JakeWharton/RxReplayingShare to disconnect when unsubscribed
        return cache.computeIfAbsentSynchronisedFunction(address) { addr ->
            atomStore.getAtoms(address)
                .flatMapIterable(Atom::getConsumables)
                .filter { particle -> particle.getOwnersPublicKeys().asSequence().all(address::ownsKey) }
                .cache()
                //.replay(1)
                //.autoConnect()
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
