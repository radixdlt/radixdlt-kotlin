package com.radixdlt.client.core.network

import io.reactivex.Observable

interface PeerDiscovery {
    fun findPeers(): Observable<RadixPeer>
}