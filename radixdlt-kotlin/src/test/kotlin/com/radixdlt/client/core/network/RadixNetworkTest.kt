package com.radixdlt.client.core.network

import io.reactivex.Observable
import org.junit.Test

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
}
