package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
import io.reactivex.Observable
import java.util.concurrent.ConcurrentHashMap

class RadixParticleStore(private val atomStore: AtomStore) : ParticleStore {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Particle>>()

    override fun getParticles(address: RadixAddress): Observable<Particle> {
        // TODO: use https://github.com/JakeWharton/RxReplayingShare to disconnect when unsubscribed
        return cache.computeIfAbsentSynchronisedFunction(address) { _ ->
            atomStore.getAtoms(address)
                .flatMapIterable(Atom::particles)
                .filter { particle -> particle.getAddresses().contains(address.publicKey) }
                .cache()
        }
    }
}
