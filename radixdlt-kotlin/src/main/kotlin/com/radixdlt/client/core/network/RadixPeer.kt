package com.radixdlt.client.core.network

import io.reactivex.Maybe
import io.reactivex.subjects.SingleSubject
import okhttp3.Request

class RadixPeer(val location: String, val isSsl: Boolean, val port: Int) {
    val radixClient: RadixJsonRpcClient
    private val data: SingleSubject<NodeRunnerData> = SingleSubject.create()

    init {

        if (isSsl) {
            this.radixClient = RadixJsonRpcClient(
                WebSocketClient(
                    (HttpClients::getSslAllTrustingClient),
                    Request.Builder().url("wss://$location:$port/rpc").build()
                )
            )
        } else {
            this.radixClient = RadixJsonRpcClient(
                WebSocketClient(
                    (HttpClients::getSslAllTrustingClient),
                    Request.Builder().url("ws://$location:$port/rpc").build()
                )
            )
        }
    }

    fun data(data: NodeRunnerData): RadixPeer {
        this.data.onSuccess(data)
        return this
    }

    fun servesShards(shards: Set<Long>): Maybe<RadixPeer> {
        return data.map { it.shards.intersects(shards) }.flatMapMaybe { if (it) Maybe.just(this) else Maybe.empty() }
    }

    override fun toString(): String {
        return location
    }
}
