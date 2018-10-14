package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
import io.reactivex.Observable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ApplicationStore<R>(private val particleStore: ParticleStore, private val reducer: ParticleReducer<R>) {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<R>>()

    fun getState(address: RadixAddress): Observable<R> {
        return cache.computeIfAbsentSynchronisedFunction(address) { addr ->
            particleStore.getParticles(address)
                .scanWith(reducer::initialState, reducer::reduce)
                .debounce(1000, TimeUnit.MILLISECONDS)
                .replay(1)
                .autoConnect()
        }
    }
}
