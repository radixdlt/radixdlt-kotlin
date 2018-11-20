package com.radixdlt.client.application.translate

import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AbstractConsumable
import com.radixdlt.client.core.atoms.AtomFeeConsumable
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import java.util.HashMap
import java.util.concurrent.TimeUnit

class AddressTokenReducer(address: RadixAddress, particleStore: ParticleStore) {

    val state: Observable<AddressTokenState>

    init {
        this.state = particleStore.getConsumables(address)
            .filter { p -> p !is AtomFeeConsumable }
            .scanWith({ HashMap<RadixHash, AbstractConsumable>() }) { map, p ->
                val newMap = HashMap(map)
                newMap[p.hash] = p
                newMap
            }
            .map { map ->
                map.values.asSequence()
                    .filter { it.isConsumable }
                    .map { it.asConsumable }
                    .toList()
            }
            .debounce(1000, TimeUnit.MILLISECONDS)
            .map { consumables ->
                val balanceInSubUnits =
                    consumables.asSequence().map { it.signedQuantity }.sum()
                val balance = Amount.subUnitsOf(balanceInSubUnits, Asset.TEST)
                AddressTokenState(balance, consumables)
            }
            .replay(1)
            .autoConnect()
    }
}
