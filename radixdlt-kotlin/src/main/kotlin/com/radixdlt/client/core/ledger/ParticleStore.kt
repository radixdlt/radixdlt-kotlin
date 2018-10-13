package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.particles.Consumable
import io.reactivex.Observable

interface ParticleStore {
    fun getConsumables(address: RadixAddress): Observable<Consumable>
}
