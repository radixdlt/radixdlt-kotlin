package com.radixdlt.client.core.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.Shards
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import com.radixdlt.client.core.network.WebSocketClient.RadixClientStatus
import com.radixdlt.client.core.serialization.RadixJson
import com.radixdlt.client.util.any
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.ReplaySubject
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

class RadixJsonRpcClientTest {

    @Test
    fun getSelfTestError() {
        val wsClient = mock(WebSocketClient::class.java)
        `when`(wsClient.getStatus()).thenReturn(Observable.just(RadixClientStatus.OPEN))

        val messages = ReplaySubject.create<String>()
        `when`(wsClient.getMessages()).thenReturn(messages)
        `when`(wsClient.connect()).thenReturn(Completable.complete())
        `when`(wsClient.send(any())).thenReturn(false)

        val jsonRpcClient = RadixJsonRpcClient(wsClient)

        val observer = TestObserver<NodeRunnerData>()

        jsonRpcClient.self.subscribe(observer)

        observer.assertValueCount(0)
        observer.assertError { t -> true }
    }

    @Test
    fun getSelfTest() {
        val wsClient = mock(WebSocketClient::class.java)
        `when`(wsClient.getStatus()).thenReturn(Observable.just(RadixClientStatus.OPEN))

        val messages = ReplaySubject.create<String>()
        `when`(wsClient.getMessages()).thenReturn(messages)
        `when`(wsClient.connect()).thenReturn(Completable.complete())

        val parser = JsonParser()
        val gson = RadixJson.gson

        doAnswer { invocation ->
            val msg = invocation.arguments[0] as String
            val jsonObject = parser.parse(msg).asJsonObject
            val id = jsonObject.get("id").asString

            val data = JsonObject()
            val system = JsonObject()
            val shards = JsonObject()
            shards.addProperty("low", -1)
            shards.addProperty("high", 1)
            system.add("shards", shards)
            data.add("system", system)

            val response = JsonObject()
            response.addProperty("id", id)
            response.add("result", data)

            messages.onNext(gson.toJson(response))
            true
        }.`when`(wsClient).send(any())
        val jsonRpcClient = RadixJsonRpcClient(wsClient)

        val observer = TestObserver<NodeRunnerData>()

        jsonRpcClient.self.subscribe(observer)

        observer.assertValueCount(1)
        observer.assertValue { data -> data.shards == Shards.range(-1, 1) }
    }

    @Test
    fun getAtomDoesNotExistTest() {
        val wsClient = mock(WebSocketClient::class.java)
        `when`(wsClient.getStatus()).thenReturn(Observable.just(RadixClientStatus.OPEN))

        val messages = ReplaySubject.create<String>()
        `when`(wsClient.getMessages()).thenReturn(messages)
        `when`(wsClient.connect()).thenReturn(Completable.complete())

        val parser = JsonParser()
        val gson = RadixJson.gson

        doAnswer { invocation ->
            val msg = invocation.arguments[0] as String
            val jsonObject = parser.parse(msg).asJsonObject
            val id = jsonObject.get("id").asString

            val atoms = JsonArray()

            val response = JsonObject()
            response.addProperty("id", id)
            response.add("result", atoms)

            messages.onNext(gson.toJson(response))
            true
        }.`when`(wsClient).send(any())
        val jsonRpcClient = RadixJsonRpcClient(wsClient)

        val observer = TestObserver<Atom>()

        jsonRpcClient.getAtom(EUID(1)).subscribe(observer)

        observer.assertValueCount(0)
        observer.assertComplete()
        observer.assertNoErrors()
    }

    @Test
    fun getAtomTest() {
        val wsClient = mock(WebSocketClient::class.java)
        `when`(wsClient.getStatus()).thenReturn(Observable.just(RadixClientStatus.OPEN))

        val messages = ReplaySubject.create<String>()
        `when`(wsClient.getMessages()).thenReturn(messages)
        `when`(wsClient.connect()).thenReturn(Completable.complete())

        val parser = JsonParser()
        val gson = RadixJson.gson

        doAnswer { invocation ->
            val msg = invocation.arguments[0] as String
            val jsonObject = parser.parse(msg).asJsonObject
            val id = jsonObject.get("id").asString

            val atoms = JsonArray()
            val atom = Atom("Test", emptyList(), emptySet(), null, null, 1)
            atoms.add(gson.toJsonTree(atom, Atom::class.java))

            val response = JsonObject()
            response.addProperty("id", id)
            response.add("result", atoms)

            messages.onNext(gson.toJson(response))
            true
        }.`when`(wsClient).send(any())
        val jsonRpcClient = RadixJsonRpcClient(wsClient)

        val observer = TestObserver<Atom>()

        jsonRpcClient.getAtom(EUID(1)).subscribe(observer)

        observer.assertValue { atom -> atom.applicationId == "Test" }
        observer.assertComplete()
        observer.assertNoErrors()
    }

    @Test
    fun getAtomsTest() {
        val wsClient = mock(WebSocketClient::class.java)
        `when`(wsClient.getStatus()).thenReturn(Observable.just(RadixClientStatus.OPEN))

        val messages = ReplaySubject.create<String>()
        `when`(wsClient.getMessages()).thenReturn(messages)
        `when`(wsClient.connect()).thenReturn(Completable.complete())

        val parser = JsonParser()
        val gson = RadixJson.gson

        doAnswer { invocation ->
            val msg = invocation.arguments[0] as String
            val jsonObject = parser.parse(msg).asJsonObject
            val id = jsonObject.get("id").asString

            val response = JsonObject()
            response.addProperty("id", id)
            response.add("result", JsonObject())

            messages.onNext(gson.toJson(response))

            val subscriberId = jsonObject.get("params").asJsonObject.get("subscriberId").asString
            val notification = JsonObject()
            notification.addProperty("method", "Atoms.subscribeUpdate")
            val params = JsonObject()
            params.addProperty("subscriberId", subscriberId)

            val atoms = JsonArray()
            val atom = gson.toJsonTree(
                Atom("Test", emptyList(), emptySet(), null, null, 1),
                Atom::class.java
            )
            atoms.add(atom)
            params.add("atoms", atoms)

            notification.add("params", params)

            messages.onNext(gson.toJson(notification))
            true
        }.`when`(wsClient).send(any())
        val jsonRpcClient = RadixJsonRpcClient(wsClient)

        val observer = TestObserver<Atom>()

        jsonRpcClient.getAtoms(AtomQuery(EUID(1))).subscribe(observer)

        observer.assertNoErrors()
        observer.assertValueCount(1)
        observer.assertValue { atom -> atom.applicationId == "Test" }
    }

    @Test
    fun getAtomsCancelTest() {
        val wsClient = mock(WebSocketClient::class.java)
        `when`(wsClient.getStatus()).thenReturn(Observable.just(RadixClientStatus.OPEN))

        val messages = ReplaySubject.create<String>()
        `when`(wsClient.getMessages()).thenReturn(messages)
        `when`(wsClient.connect()).thenReturn(Completable.complete())

        val parser = JsonParser()
        val gson = RadixJson.gson

        doAnswer { invocation ->
            val msg = invocation.arguments[0] as String
            val jsonObject = parser.parse(msg).asJsonObject
            val id = jsonObject.get("id").asString
            val method = jsonObject.get("method").asString

            if (method == "Atoms.subscribe") {

                val response = JsonObject()
                response.addProperty("id", id)
                response.add("result", JsonObject())

                messages.onNext(gson.toJson(response))
            } else if (method == "Subscription.cancel") {
                val subscriberId = jsonObject.get("params").asJsonObject.get("subscriberId").asString
                val notification = JsonObject()
                notification.addProperty("method", "Atoms.subscribeUpdate")
                val params = JsonObject()
                params.addProperty("subscriberId", subscriberId)
                val atoms = JsonArray()
                val atom = gson.toJsonTree(
                    Atom("Test", emptyList(), emptySet(), null, null, 1),
                    Atom::class.java
                )
                atoms.add(atom)
                params.add("atoms", atoms)

                notification.add("params", params)

                messages.onNext(gson.toJson(notification))
            }

            true
        }.`when`(wsClient).send(any())
        val jsonRpcClient = RadixJsonRpcClient(wsClient)

        val observer = TestObserver<Atom>()

        jsonRpcClient.getAtoms(AtomQuery(EUID(1)))
            .subscribe(observer)
        observer.cancel()

        observer.assertSubscribed()
        observer.assertNoErrors()
        observer.assertValueCount(0)
    }

    @Test
    fun submitAtomTest() {
        val wsClient = mock(WebSocketClient::class.java)
        `when`(wsClient.getStatus()).thenReturn(Observable.just(RadixClientStatus.OPEN))

        val messages = ReplaySubject.create<String>()
        `when`(wsClient.getMessages()).thenReturn(messages)
        `when`(wsClient.connect()).thenReturn(Completable.complete())

        val parser = JsonParser()
        val gson = RadixJson.gson

        doAnswer { invocation ->
            val msg = invocation.arguments[0] as String
            val jsonObject = parser.parse(msg).asJsonObject
            val id = jsonObject.get("id").asString
            val method = jsonObject.get("method").asString

            if (method == "Universe.submitAtomAndSubscribe") {
                val response = JsonObject()
                response.addProperty("id", id)
                response.add("result", JsonObject())

                messages.onNext(gson.toJson(response))

                val subscriberId = jsonObject.get("params").asJsonObject.get("subscriberId").asString
                val notification = JsonObject()
                notification.addProperty("method", "AtomSubmissionState.onNext")
                val params = JsonObject()
                params.addProperty("subscriberId", subscriberId)
                params.addProperty("value", "STORED")

                notification.add("params", params)

                messages.onNext(gson.toJson(notification))
            }

            true
        }.`when`(wsClient).send(any())
        val jsonRpcClient = RadixJsonRpcClient(wsClient)

        val observer = TestObserver<AtomSubmissionUpdate>()

        jsonRpcClient.submitAtom(
            Atom("Test", emptyList(), emptySet(), null, null, 1)
        ).subscribe(observer)

        observer.assertNoErrors()
        observer.assertValueAt(observer.valueCount() - 1) { update -> update.getState() == AtomSubmissionState.STORED }
        observer.assertComplete()
    }
}
