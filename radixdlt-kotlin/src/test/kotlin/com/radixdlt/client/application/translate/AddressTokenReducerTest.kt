package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.Spin
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test

class AddressTokenReducerTest {

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

        whenever(store.getConsumables(address)).thenReturn(
            Observable.just<Consumable>(consumable).concatWith(Observable.never())
        )
        val reducer = AddressTokenReducer(address, store)

        val testObserver = TestObserver.create<AddressTokenState>()
        reducer.state.subscribe(testObserver)
        testObserver.awaitCount(1)
        testObserver.assertValue { state -> state.balance.amountInSubunits == 10L }
        testObserver.dispose()

        val testObserver2 = TestObserver.create<AddressTokenState>()
        reducer.state.subscribe(testObserver2)
        testObserver2.assertValue { state -> state.balance.amountInSubunits == 10L }

        verify(store, times(1)).getConsumables(address)
    }
}
