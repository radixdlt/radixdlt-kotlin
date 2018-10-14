package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
import io.reactivex.Observable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TokenBalanceReducer(private val particleStore: ParticleStore) {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<TokenBalanceState>>()

    fun getState(address: RadixAddress): Observable<TokenBalanceState> {
        return cache.computeIfAbsentSynchronisedFunction(address) { _ ->
            particleStore.getParticles(address)
                .filter { p -> p is Consumable && p !is AtomFeeConsumable }
                .map { p -> p as Consumable }
                .scanWith(::TokenBalanceState, TokenBalanceState.Companion::merge)
                .debounce(1000, TimeUnit.MILLISECONDS)
                .replay(1)
                .autoConnect()
        }
    }
}
