package com.radixdlt.client.core.network

import io.reactivex.Observable
import io.reactivex.Single

class SinglePeer(private val peer: String, private val useSSL: Boolean, private val port: Int) : PeerDiscovery {

    override fun findPeers(): Observable<RadixPeer> {
        return Single.fromCallable { RadixPeer(peer, useSSL, port) }
                .flatMap { peer ->
                    peer.radixClient.self.map { data ->
                        peer.data(data)
                        peer
                    }
                }.toObservable()
    }
}
