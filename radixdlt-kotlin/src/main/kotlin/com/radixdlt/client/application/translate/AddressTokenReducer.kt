package com.radixdlt.client.application.translate

import com.radixdlt.client.application.objects.Amount
import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import java.util.HashMap
import java.util.concurrent.TimeUnit

class AddressTokenReducer(address: RadixAddress, particleStore: ParticleStore) {

    val state: Observable<AddressTokenState>

    init {
        this.state = particleStore.getConsumables(address)
            .filter { p -> p !is AtomFeeConsumable }
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
                val balanceInSubUnits =
                    consumables.asSequence().map(Consumable::amount).sum()
                val balance = Amount.subUnitsOf(balanceInSubUnits, Token.TEST)
                AddressTokenState(balance, consumables)
            }
            .replay(1)
            .autoConnect()
    }
}
