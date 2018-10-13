package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.particles.Particle
import io.reactivex.Observable
import java.util.concurrent.ConcurrentHashMap

class RadixParticleStore(private val atomStore: AtomStore) : ParticleStore {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Particle>>()

    override fun getParticles(address: RadixAddress): Observable<Particle> {
        // TODO: use https://github.com/JakeWharton/RxReplayingShare to disconnect when unsubscribed
        return cache.computeIfAbsentSynchronisedFunction(address) { addr ->
            atomStore.getAtoms(address)
                .flatMapIterable(Atom::particles)
                .filter { particle -> particle.getAddresses().contains(address.publicKey) }
                .cache()
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
