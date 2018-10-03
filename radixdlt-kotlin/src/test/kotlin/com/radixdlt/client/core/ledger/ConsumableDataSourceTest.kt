package com.radixdlt.client.core.ledger

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AtomObservation
import com.radixdlt.client.core.atoms.Consumable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test

class ConsumableDataSourceTest {

    @Test
    fun testEmptyConsumables() {
        val atomStore = mock<(EUID) -> (Observable<AtomObservation>)>()
        val observation = mock<AtomObservation>()
        whenever(observation.isHead).thenReturn(true)
        val address = mock<RadixAddress>()
        whenever(atomStore(anyOrNull())).thenReturn(Observable.just(observation));
        val consumableDataSource = ConsumableDataSource(atomStore)
        val testObserver = TestObserver.create<Collection<Consumable>>()
        consumableDataSource.getConsumables(address).subscribe(testObserver)
        testObserver.assertValue { it.isEmpty() }
    }
}
