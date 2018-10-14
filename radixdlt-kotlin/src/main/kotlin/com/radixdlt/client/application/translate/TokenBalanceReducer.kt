package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
import io.reactivex.Observable
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TokenBalanceReducer(private val particleStore: ParticleStore) {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<TokenBalanceState>>()

    fun getState(address: RadixAddress): Observable<TokenBalanceState> {
        return cache.computeIfAbsentSynchronisedFunction(address) { _ ->
            particleStore.getParticles(address)
                .filter { p -> p is Consumable && p !is AtomFeeConsumable }
                .map { p -> p as Consumable }
                .scanWith( { HashMap<RadixHash, Consumable>() }) { map, p ->
                    val newMap = HashMap(map)
                    newMap[p.getHash()] = p
                    newMap
                }
                .map { map ->
                    map.values.asSequence()
                        .filter { c -> c.getSpin() === Spin.UP }
                        .toList()
                }
                .debounce(1000, TimeUnit.MILLISECONDS)
                .map { consumables ->

                    val balance: Map<TokenRef, Long> = consumables
                        .asSequence()
                        .groupBy(Consumable::tokenRef) {
                            it.amount
                        }.mapValues {
                            it.value.sum()
                        }

                    val consumableLists: Map<TokenRef, List<Consumable>> = consumables
                        .asSequence()
                        .groupBy(Consumable::tokenRef)

                    TokenBalanceState(balance, consumableLists)
                }
                .replay(1)
                .autoConnect()
        }
    }
}
