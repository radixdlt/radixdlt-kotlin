package com.radixdlt.client.core.ledger

import com.nhaarman.mockitokotlin2.any
import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.network.RadixJsonRpcClient
import com.radixdlt.client.core.network.RadixNetwork
import com.radixdlt.client.core.network.WebSocketClient.RadixClientStatus
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.IOException

class ClientSelectorTest {
    @Test
    fun failedNodeConnectionTest() {
        val config = mock<RadixUniverseConfig>(RadixUniverseConfig::class.java)
        val network = mock<RadixNetwork>(RadixNetwork::class.java)
        val client = mock<RadixJsonRpcClient>(RadixJsonRpcClient::class.java)
        `when`(client.status).thenReturn(Observable.just(RadixClientStatus.OPEN))
        `when`(client.getUniverse()).thenReturn(Single.error<RadixUniverseConfig>(IOException()))
        `when`(network.getRadixClients(any<Set<Long>>())).thenReturn(
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
}
