package com.radixdlt.client.core.network

import com.radixdlt.client.util.any
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.NoSuchElementException

import java.util.stream.IntStream

class RadixNetworkTest {

    @Test
    fun testGetClientsMultipleTimes() {

        val network = RadixNetwork(object : PeerDiscovery {
            override fun findPeers(): Observable<RadixPeer> {
                return Observable.just(
                        RadixPeer("1", false, 8080),
                        RadixPeer("2", false, 8080),
                        RadixPeer("3", false, 8080)
                )
            }
        })

        IntStream.range(0, 10).forEach { _ ->
            network.radixClients
                    .map { it.location }
                    .test()
                    .assertValueAt(0, "http://1:8080/rpc")
                    .assertValueAt(1, "http://2:8080/rpc")
                    .assertValueAt(2, "http://3:8080/rpc")
        }
    }

    @Test
    fun testAPIMismatch() {
        val peer = mock<RadixPeer>(RadixPeer::class.java)
        val client = mock<RadixJsonRpcClient>(RadixJsonRpcClient::class.java)
        `when`<Maybe<RadixPeer>>(peer.servesShards(any())).thenReturn(Maybe.just<RadixPeer>(peer))
        `when`<RadixJsonRpcClient>(peer.radixClient).thenReturn(client)
        `when`<Observable<WebSocketClient.RadixClientStatus>>(client.status).thenReturn(
            Observable.just(
                WebSocketClient.RadixClientStatus.OPEN
            )
        )
        `when`<Single<Boolean>>(client.checkAPIVersion()).thenReturn(Single.just(false))

        // SAM could be replaced in Kotlin by higher order function
        val network = RadixNetwork(object : PeerDiscovery {
            override fun findPeers(): Observable<RadixPeer> {
                return Observable.just(peer)
            }
        })

        val testObserver = TestObserver.create<RadixJsonRpcClient>()
        network.getRadixClient(0L).subscribe(testObserver)
        testObserver.assertError(NoSuchElementException::class.java)
    }
}
