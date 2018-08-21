package com.radixdlt.client.core.network

import io.reactivex.Observable
import io.reactivex.Single
import org.slf4j.LoggerFactory
import java.util.ArrayList

class PeersFromSeed(private val seed: RadixPeer) : PeerDiscovery {

    private val LOGGER = LoggerFactory.getLogger(PeersFromSeed::class.java)

    override fun findPeers(): Observable<RadixPeer> {
        val rawSeed = Single.just(seed).cache()
        val connectedSeed = rawSeed
            .doOnSuccess { seed ->
                seed.radixClient.self.subscribe({ seed.data(it) }) {
                    LOGGER.warn("Unable to load seed info")
                }
            }
            .toObservable()

        return Observable.concat(
            connectedSeed
                .map<RadixJsonRpcClient> { it.radixClient }
                .flatMapSingle { client -> client.livePeers.doFinally { client.tryClose() } }
                .doOnNext { list -> LOGGER.info("Got peer list $list") }
                .flatMapIterable { list ->
                    val copyList = ArrayList(list)
                    copyList.shuffle()
                    copyList
                }
                .map { data -> RadixPeer(data.ip!!, seed.isSsl, seed.port).data(data) },
            rawSeed.toObservable()
        ).distinct<String> { it.location }
    }
}
