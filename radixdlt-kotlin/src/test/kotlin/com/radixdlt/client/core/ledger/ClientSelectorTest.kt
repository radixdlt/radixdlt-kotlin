package com.radixdlt.client.core.ledger

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.network.RadixJsonRpcClient
import com.radixdlt.client.core.network.RadixNetwork
import com.radixdlt.client.core.network.WebSocketClient.RadixClientStatus
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClientSelectorTest {
    @Test
    fun failedNodeConnectionTest() {
        val config = mock<RadixUniverseConfig>()
        val network = mock<RadixNetwork>()
        val client = mock<RadixJsonRpcClient>()
        whenever(client.status).thenReturn(Observable.just(RadixClientStatus.OPEN))
        whenever(client.getUniverse()).thenReturn(Single.error<RadixUniverseConfig>(IOException()))
        whenever(network.getRadixClients(any<Set<Long>>())).thenReturn(
            Observable.concat(
                Observable.just(client),
                Observable.never()
            )
        )

        val clientSelector = ClientSelector(config, network)
        val testObserver = TestObserver.create<RadixJsonRpcClient>()
        clientSelector.getRadixClient(1L).subscribe(testObserver)

        testObserver.assertNoErrors()
        testObserver.assertNoValues()
    }

    @Test
    fun dontConnectToAllNodesTest() {
        val config = mock<RadixUniverseConfig>()

        val network = mock<RadixNetwork>()

        val clients = (0..100).asSequence().map { i ->
            val client = mock<RadixJsonRpcClient>()
            whenever(client.status).thenReturn(Observable.just(RadixClientStatus.CLOSED))
            if (i == 0) {
                whenever(client.getUniverse()).thenReturn(
                    Single.timer(
                        1,
                        TimeUnit.SECONDS
                    ).map { _ -> config })
            } else {
                whenever(client.getUniverse()).thenReturn(Single.never<RadixUniverseConfig>())
            }
            client
        }.toList()
        whenever(network.getRadixClients(any<Set<Long>>()))
            .thenReturn(Observable.fromIterable(clients))

        val clientSelector = ClientSelector(config, network)
        val testObserver = TestObserver.create<RadixJsonRpcClient>()
        clientSelector.getRadixClient(1L).subscribe(testObserver)
        testObserver.awaitTerminalEvent()
        testObserver.assertValue(clients[0])

        verify(clients[99], times(0)).getUniverse()
    }

    @Test
    fun whenFirstNodeFailsThenSecondNodeShouldConnect() {
        val config = mock<RadixUniverseConfig>()
        val network = mock<RadixNetwork>()
        val badClient = mock<RadixJsonRpcClient>()
        whenever(badClient.status).thenReturn(Observable.just(RadixClientStatus.OPEN))
        whenever(badClient.getUniverse()).thenReturn(
            Single.error<RadixUniverseConfig>(
                IOException()
            )
        )

        val goodClient = mock<RadixJsonRpcClient>()
        whenever(goodClient.status).thenReturn(Observable.just(RadixClientStatus.OPEN))
        whenever(goodClient.getUniverse()).thenReturn(
            Single.just<RadixUniverseConfig>(
                mock()
            )
        )

        whenever(network.getRadixClients(any<Set<Long>>())).thenReturn(
            Observable.concat(
                Observable.just(badClient),
                Observable.just(goodClient),
                Observable.never<RadixJsonRpcClient>()
            )
        )

        val clientSelector = ClientSelector(config, network)
        val testObserver = TestObserver.create<RadixJsonRpcClient>()
        clientSelector.getRadixClient(1L).subscribe(testObserver)

        testObserver.awaitTerminalEvent()
        testObserver.assertNoErrors()
        testObserver.assertValue(goodClient)
    }
}
