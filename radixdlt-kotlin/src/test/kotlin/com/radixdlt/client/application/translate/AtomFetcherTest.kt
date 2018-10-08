package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.ledger.AtomFetcher
import com.radixdlt.client.core.network.RadixJsonRpcClient
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import java.math.BigInteger

class AtomFetcherTest {
    @Test
    fun firstNodeGetAtomsFail() {
        val bad = mock<RadixJsonRpcClient>()
        val good = mock<RadixJsonRpcClient>()
        val atom = mock<Atom>()
        whenever(atom.timestamp).thenReturn(1L)

        whenever<Observable<Atom>>(bad.getAtoms(any())).thenReturn(
            Observable.error<Atom>(RuntimeException())
        )
        whenever<Observable<Atom>>(good.getAtoms(any())).thenReturn(Observable.just<Atom>(atom))

        val clientSelector = mock<(Long) -> (Single<RadixJsonRpcClient>)>()
        whenever(clientSelector(any())).thenReturn(
            Single.just<RadixJsonRpcClient>(bad), Single.just<RadixJsonRpcClient>(good)
        )

        val atomFetcher = AtomFetcher(clientSelector)
        val testObserver = TestObserver.create<Atom>()
        atomFetcher.fetchAtoms(EUID(BigInteger.ONE)).subscribe(testObserver)
        testObserver.awaitCount(1)
        testObserver.assertValue(atom)
    }
}
