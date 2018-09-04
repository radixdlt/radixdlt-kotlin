package com.radixdlt.client.core.network

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.IOException

class PeersFromSeedTest {
    @Test
    fun testFindPeers() {
        val peer = mock(RadixPeer::class.java)
        val client = mock(RadixJsonRpcClient::class.java)
        val data = mock(NodeRunnerData::class.java)
        `when`(peer.radixClient).thenReturn(client)
        `when`(peer.location).thenReturn("somewhere")
        `when`(client.self).thenReturn(Single.just(data))
        `when`(client.livePeers).thenReturn(Single.just(emptyList()))

        val testObserver = TestObserver.create<RadixPeer>()
        val peersFromSeed = PeersFromSeed(peer)
        peersFromSeed.findPeers().subscribe(testObserver)

        testObserver.assertValue { p -> p.location == "somewhere" }
    }

    @Test
    fun testFindPeersFail() {
        val peer = mock(RadixPeer::class.java)
        val client = mock(RadixJsonRpcClient::class.java)
        `when`(peer.radixClient).thenReturn(client)
        `when`(peer.location).thenReturn("somewhere")
        `when`(client.self).thenReturn(Single.error(IOException()))
        `when`(client.livePeers).thenReturn(Single.error(IOException()))

        val testObserver = TestObserver.create<RadixPeer>()
        val peersFromSeed = PeersFromSeed(peer)
        peersFromSeed.findPeers().subscribe(testObserver)

        testObserver.assertError { e -> e is IOException }
    }
}
