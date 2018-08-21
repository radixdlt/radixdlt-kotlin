package com.radixdlt.client.core.network

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketClient(private val okHttpClient: () -> OkHttpClient, val endpoint: Request) {

    private var webSocket: WebSocket? = null

    private val status = BehaviorSubject.createDefault(RadixClientStatus.CLOSED)
    private val closed = AtomicBoolean(false)

    private var messages = PublishSubject.create<String>()

    enum class RadixClientStatus {
        CONNECTING, OPEN, CLOSED, FAILURE
    }

    init {
        this.status
            .filter { status -> status == RadixClientStatus.FAILURE }
            .debounce(1, TimeUnit.MINUTES)
            .subscribe {
                this.messages = PublishSubject.create()
                this.status.onNext(RadixClientStatus.CLOSED)
            }
    }

    fun getMessages(): Observable<String> {
        return messages
    }

    fun getStatus(): Observable<RadixClientStatus> {
        return status
    }

    fun close(): Boolean {
        if (messages.hasObservers()) {
            return false
        }

        this.webSocket?.close(1000, null)

        return true
    }

    private fun tryConnect() {
        // TODO: Race condition here but not fatal, fix later on
        if (this.status.value == RadixClientStatus.CONNECTING) {
            return
        }

        this.status.onNext(RadixClientStatus.CONNECTING)

        // HACKISH: fix
        this.webSocket = this.okHttpClient().newWebSocket(endpoint, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                this@WebSocketClient.status.onNext(RadixClientStatus.OPEN)
            }

            override fun onMessage(webSocket: WebSocket?, message: String?) {
                messages.onNext(message!!)
            }

            override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                webSocket!!.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                this@WebSocketClient.status.onNext(RadixClientStatus.CLOSED)
            }

            override fun onFailure(websocket: WebSocket?, t: Throwable?, response: Response?) {
                if (closed.get()) {
                    return
                }

                LOGGER.error(t!!.toString())
                this@WebSocketClient.status.onNext(RadixClientStatus.FAILURE)

                this@WebSocketClient.messages.onError(IOException())
            }
        })
    }

    /**
     * Attempts to connect to this Radix node on subscribe if not already connected
     *
     * @return completable which signifies when connection has been made
     */
    fun connect(): Completable {
        return this.getStatus()
            .doOnNext { status ->
                // TODO: cancel tryConnect on dispose
                if (status == RadixClientStatus.CLOSED) {
                    this.tryConnect()
                } else if (status == RadixClientStatus.FAILURE) {
                    throw IOException()
                }
            }
            .filter { status -> status == RadixClientStatus.OPEN }
            .firstOrError()
            .ignoreElement()
    }

    fun send(message: String): Boolean {
        return this.webSocket!!.send(message)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(WebSocketClient::class.java)
    }
}
