package com.radixdlt.client.core.network

import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class PeersFromNodeFinder(private val nodeFinderUrl: String, private val port: Int) : PeerDiscovery {

    override fun findPeers(): Observable<RadixPeer> {
        val request = Request.Builder()
            .url(this.nodeFinderUrl)
            .build()

        return Single.create<String> { emitter ->
            val call = HttpClients.getSslAllTrustingClient().newCall(request)
            emitter.setCancellable { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    emitter.tryOnError(e)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        val bodyString = body.string()
                        body.close()
                        if (bodyString.isEmpty()) {
                            emitter.tryOnError(IOException("Received empty peer."))
                        } else {
                            emitter.onSuccess(bodyString)
                        }
                    } else {
                        emitter.tryOnError(IOException("Error retrieving peer: " + response.message()))
                    }
                }
            })
        }
            .map { peerUrl -> PeersFromSeed(RadixPeer(peerUrl, true, port)) }
            .flatMapObservable(PeersFromSeed::findPeers)
            .timeout(3, TimeUnit.SECONDS)
            .retryWhen(IncreasingRetryTimer())
    }
}
