package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test

class ApplicationStoreTest {

    @Test
    fun testCache() {
        val address = mock<RadixAddress>()
        val store = mock<ParticleStore>()
        val consumable = mock<Consumable>()
        val hash = mock<RadixHash>()
        whenever(consumable.getSignedAmount()).thenReturn(10L)
        whenever(consumable.amount).thenReturn(10L)
        whenever(consumable.getHash()).thenReturn(hash)
        whenever(consumable.getSpin()).thenReturn(Spin.UP)
        whenever(consumable.getDson()).thenReturn(byteArrayOf(1))
        val token = mock<TokenRef>()
        whenever(consumable.tokenRef).thenReturn(token)

        whenever(store.getParticles(address)).thenReturn(
            Observable.just<Particle>(consumable).concatWith(Observable.never())
        )

        val o = mock<Any>()

        val reducer = mock<ParticleReducer<Any>>()
        whenever(reducer.initialState()).thenReturn(o)
        whenever(reducer.reduce(any(), any())).thenReturn(o)
        val applicationStore = ApplicationStore(store, reducer)

        val testObserver = TestObserver.create<Any>()
        applicationStore.getState(address).subscribe(testObserver)
        testObserver.awaitCount(1)
        testObserver.assertValue(o)
        testObserver.dispose()

        val testObserver2 = TestObserver.create<Any>()
        applicationStore.getState(address).subscribe(testObserver2)
        testObserver2.assertValue(o)

        verify(store, times(1)).getParticles(address)
    }
}
