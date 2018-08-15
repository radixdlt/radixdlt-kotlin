package com.radixdlt.client.core.network


import com.radixdlt.client.util.any
import io.reactivex.observers.TestObserver
import okhttp3.*
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import java.io.IOException

class WebSocketClientTest {
    @Test
    fun testConnect() {
        val okHttpClient = mock(OkHttpClient::class.java)
        val webSocket = mock(WebSocket::class.java)
        val request = mock(Request::class.java)
        val response = mock(Response::class.java)

        val client = WebSocketClient({ okHttpClient }, request)
        doAnswer { invocation ->
            val listener = invocation.arguments[1] as WebSocketListener
            listener.onOpen(webSocket, response)
            webSocket
        }.`when`(okHttpClient).newWebSocket(any(), any())

        val testObserver = TestObserver.create<Any>()
        client.connect().subscribe(testObserver)
        testObserver.assertComplete()
    }

    @Test
    fun testConnectFail() {
        val okHttpClient = mock(OkHttpClient::class.java)
        val webSocket = mock(WebSocket::class.java)
        val request = mock(Request::class.java)

        val client = WebSocketClient({ okHttpClient }, request)
        doAnswer { invocation ->
            val listener = invocation.arguments[1] as WebSocketListener
            listener.onFailure(webSocket, RuntimeException(), null)
            webSocket
        }.`when`(okHttpClient).newWebSocket(any(), any())

        val testObserver = TestObserver.create<Any>()
        client.connect().subscribe(testObserver)
        testObserver.assertError { t -> t is IOException }
    }

    @Test
    fun testMessage() {
        val okHttpClient = mock(OkHttpClient::class.java)
        val webSocket = mock(WebSocket::class.java)
        val request = mock(Request::class.java)
        val response = mock(Response::class.java)

        val client = WebSocketClient({ okHttpClient }, request)
        doAnswer { invocation ->
            val listener = invocation.arguments[1] as WebSocketListener
            listener.onOpen(webSocket, response)
            listener.onMessage(webSocket, "hello")
            webSocket
        }.`when`(okHttpClient).newWebSocket(any(), any())

        val testObserver = TestObserver.create<String>()
        client.getMessages().subscribe(testObserver)
        client.connect().subscribe()

        testObserver.assertValue("hello")
    }

    @Test
    fun testMessageThenError() {
        val okHttpClient = mock(OkHttpClient::class.java)
        val webSocket = mock(WebSocket::class.java)
        val request = mock(Request::class.java)
        val response = mock(Response::class.java)

        val client = WebSocketClient({ okHttpClient }, request)
        doAnswer { invocation ->
            val listener = invocation.arguments[1] as WebSocketListener
            listener.onOpen(webSocket, response)
            listener.onMessage(webSocket, "hello")
            listener.onFailure(webSocket, RuntimeException(), null)
            webSocket
        }.`when`(okHttpClient).newWebSocket(any(), any())

        val testObserver = TestObserver.create<String>()
        client.getMessages().subscribe(testObserver)
        client.connect().subscribe()

        testObserver.assertFailure(IOException::class.java, "hello")
    }
}
