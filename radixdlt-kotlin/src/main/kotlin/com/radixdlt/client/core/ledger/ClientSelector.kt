package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.network.RadixJsonRpcClient
import com.radixdlt.client.core.network.RadixNetwork
import com.radixdlt.client.core.network.WebSocketClient.RadixClientStatus
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Given a network, selects the node to connect to
 */
class ClientSelector(
    /**
     * The Universe the node we return must match
     */
    private val config: RadixUniverseConfig,
    /**
     * The network of peers available to connect to
     */
    private val radixNetwork: RadixNetwork
) {

    /**
     * The amount of time to delay in between node connection requests
     */
    private val delaySecs: Int = 3

    /**
     * Returns a cold observable of the first peer found which supports
     * a set short shards which intersects with a given shard
     *
     * @param shard a shards to find an intersection with
     * @return a cold observable of the first matching Radix client
     */
    fun getRadixClient(shard: Long?): Single<RadixJsonRpcClient> {
        return getRadixClient(setOf(shard!!))
    }

    /**
     * Returns a cold observable of the first peer found which supports
     * a set short shards which intersects with a given set of shards.
     *
     * @param shards set of shards to find an intersection with
     * @return a cold observable of the first matching Radix client
     */
    fun getRadixClient(shards: Set<Long>): Single<RadixJsonRpcClient> {
        return this.radixNetwork.getRadixClients(shards)
            .flatMapMaybe { client ->
                client.status
                    .filter { status -> status != RadixClientStatus.FAILURE && status != RadixClientStatus.CLOSING }
                    .map { _ -> client }
                    .firstOrError()
                    .toMaybe()
                    .onErrorComplete()
            }
            .zipWith(Observable.interval(delaySecs.toLong(), TimeUnit.SECONDS)) { c, _ -> c }
            .flatMapMaybe { client ->
                client.getUniverse()
                    .doOnSuccess { cliUniverse ->
                        if (config != cliUniverse) {
                            LOGGER.warn(
                                "{} has universe: {} but looking for {}",
                                client, cliUniverse.getHash(), config.getHash()
                            )
                        }
                    }
//                    .map { config == it }
                    .filter { _ -> true }
                    .map { _ -> client }
                    .onErrorComplete()
            }
            .firstOrError()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClientSelector::class.java)
    }
}
