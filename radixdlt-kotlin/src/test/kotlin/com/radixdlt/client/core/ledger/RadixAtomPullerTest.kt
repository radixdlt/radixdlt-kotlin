package com.radixdlt.client.core.ledger

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AtomObservation
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.math.BigInteger

class RadixAtomPullerTest {
    @Test
    @Throws(Exception::class)
    fun testClientAPICalledOnceWithManySubscibers() {
        @Suppress("UNCHECKED_CAST")
        val onSubscribe: Consumer<Disposable> = mock(Consumer::class.java) as Consumer<Disposable>
        val atoms: Observable<AtomObservation> = Observable.never<AtomObservation>().doOnSubscribe(onSubscribe)
        val fetcher = mock<(EUID) -> (Observable<AtomObservation>)>()
        `when`(fetcher(any())).thenReturn(atoms)

        val radixAtomPuller = RadixAtomPuller(fetcher) { _, _ -> }

        val observers = generateSequence(TestObserver.create<Any>()) { TestObserver.create() }
            .take(10)
            .toList()

        observers.forEach { _ -> radixAtomPuller.pull(EUID(BigInteger.ONE)) }

        verify(onSubscribe, times(1)).accept(any())
    }
}
