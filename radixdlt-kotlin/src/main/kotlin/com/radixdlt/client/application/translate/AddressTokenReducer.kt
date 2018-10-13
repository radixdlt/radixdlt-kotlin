package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import java.util.HashMap
import java.util.concurrent.TimeUnit

class AddressTokenReducer(address: RadixAddress, particleStore: ParticleStore) {

    val state: Observable<AddressTokenState>

    init {
        this.state = particleStore.getParticles(address)
            .filter { p -> p is Consumable && p !is AtomFeeConsumable }
            .map { p -> p as Consumable }
            .scanWith( { HashMap<RadixHash, Consumable>() }) { map, p ->
                val newMap = HashMap(map)
                newMap[p.getHash()] = p
                newMap
            }
            .map { map ->
                map.values.asSequence()
                    .filter { c -> c.getSpin() == Spin.UP }
                    .toList()
            }
            .debounce(1000, TimeUnit.MILLISECONDS)
            .map { consumables ->

                val balance: Map<EUID, Long> = consumables
                    .asSequence()
                    .groupBy(Consumable::getTokenClass) {
                        it.amount
                    }.mapValues {
                        it.value.sum()
                    }

                val consumableLists: Map<EUID, List<Consumable>> = consumables
                    .asSequence()
                    .groupBy(Consumable::getTokenClass)

                AddressTokenState(balance, consumableLists)
            }
            .replay(1)
            .autoConnect()
    }
}
