package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.particles.Particle
import io.reactivex.Observable

interface ParticleStore {
    fun getParticles(address: RadixAddress): Observable<Particle>
}
