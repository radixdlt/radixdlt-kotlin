package com.radixdlt.client.core.network

import com.radixdlt.client.core.network.WebSocketClient.RadixClientStatus
import io.reactivex.Observable
import io.reactivex.observables.ConnectableObservable
import org.slf4j.LoggerFactory
import java.util.AbstractMap.SimpleImmutableEntry
import java.util.Objects

/**
 * A Radix Network manages connections to Node Runners for a given Universe.
 */
class RadixNetwork(peerDiscovery: PeerDiscovery) {

    /**
     * Cached observable for keeping track of Radix Peers
     */
    private val peers: Observable<RadixPeer>

    /**
     * Hot observable which updates subscribers of new connection events
     */
    private val statusUpdates: ConnectableObservable<SimpleImmutableEntry<String, RadixClientStatus>>

    init {
        Objects.requireNonNull(peerDiscovery)

        this.peers = peerDiscovery.findPeers()
            .retryWhen(IncreasingRetryTimer())
            .doOnNext { peer -> LOGGER.info("Added to peer list: " + peer.location) }
            .replay().autoConnect(2)

        this.statusUpdates = peers.map { it.radixClient }
            .flatMap { client -> client.status.map { status -> SimpleImmutableEntry(client.location, status) } }
            .publish()
        this.statusUpdates.connect()
    }

    fun connectAndGetStatusUpdates(): Observable<SimpleImmutableEntry<String, RadixClientStatus>> {
        this.peers.subscribe()
        return this.getStatusUpdates()
    }

    /**
     * Returns a hot observable of the status of peers
     *
     * @return a hot Observable of status of peers
     */
    fun getStatusUpdates(): Observable<SimpleImmutableEntry<String, RadixClientStatus>> {
        return statusUpdates
    }

    fun getRadixClients(): Observable<RadixJsonRpcClient> {
        return peers.map(RadixPeer::radixClient)
    }

    fun getRadixClients(shards: Set<Long>): Observable<RadixJsonRpcClient> {
        return peers.flatMapMaybe { peer -> peer.servesShards(shards) }
            .map(RadixPeer::radixClient)
            .flatMapMaybe { client -> client.checkAPIVersion().filter { b -> b }.map { _ -> client } }
    }

    fun getRadixClients(shard: Long): Observable<RadixJsonRpcClient> {
        return this.getRadixClients(setOf(shard))
    }

    /**
     * Free resources cleanly to the best of our ability
     */
    fun close() {
        // TODO: fix concurrency
        // TODO: Cleanup objects, etc.
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RadixNetwork::class.java)
    }
}
