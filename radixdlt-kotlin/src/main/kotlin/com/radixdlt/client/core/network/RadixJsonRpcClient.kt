package com.radixdlt.client.core.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import com.radixdlt.client.core.network.WebSocketClient.RadixClientStatus
import com.radixdlt.client.core.serialization.RadixJson
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Responsible for managing the state across one web socket connection to a Radix Node.
 * This consists of mainly keeping track of JSON-RPC method calls and JSON-RPC subscription
 * calls.
 */
class RadixJsonRpcClient(
    /**
     * The websocket this is wrapping
     */
    private val wsClient: WebSocketClient
) {

    /**
     * Hot observable of messages received through the websocket
     */
    private val messages: Observable<JsonObject>

    /**
     * Cached API version of Node
     */
    private val serverApiVersion: Single<Int>

    /**
     * Cached Universe of Node
     */
    private val universeConfig: Single<RadixUniverseConfig>

    /**
     * @return URL which websocket is connected to
     */
    val location: String
        get() = wsClient.endpoint.url().toString()

    val status: Observable<RadixClientStatus>
        get() = wsClient.getStatus()

    /**
     * Retrieve the node data for node we are connected to
     *
     * @return node data for node we are connected to
     */
    val self: Single<NodeRunnerData>
        get() = this.jsonRpcCall("Network.getSelf")
            .map { result -> RadixJson.gson.fromJson(result, NodeRunnerData::class.java) }

    /**
     * Retrieve list of nodes this node knows about
     *
     * @return list of nodes this node knows about
     */
    val livePeers: Single<List<NodeRunnerData>>
        get() = this.jsonRpcCall("Network.getLivePeers")
            .map { result ->
                RadixJson.gson.fromJson<List<NodeRunnerData>>(result, object : TypeToken<List<NodeRunnerData>>() {
                }.type)
            }

    init {

        val parser = JsonParser()
        this.messages = this.wsClient.getMessages()
            .map { msg -> parser.parse(msg).asJsonObject }
            .publish()
            .refCount()

        if (!CHECK_API_VERSION) {
            this.serverApiVersion = Single.just(API_VERSION)
        } else {
            this.serverApiVersion = jsonRpcCall("Api.getVersion")
                .map { result -> result.asJsonObject.get("version").asInt }
                .cache()
        }

        this.universeConfig = jsonRpcCall("Universe.getUniverse")
            .map { result -> RadixJson.gson.fromJson(result, RadixUniverseConfig::class.java) }
            .cache()
    }

    /**
     * Attempts to close the websocket this client is connected to.
     * If there are still observers connected to the websocket closing
     * will not occur.
     *
     * @return true if websocket was successfully closed, false otherwise
     */
    fun tryClose(): Boolean {
        return this.wsClient.close()
    }

    /**
     * Generic helper method for calling a JSON-RPC method. Deserializes the received json.
     *
     * @param method name of JSON-RPC method
     * @return response from rpc method
     */
    private fun jsonRpcCall(method: String, params: JsonObject): Single<JsonElement> {
        return this.wsClient.connect().andThen(
            Single.create { emitter ->
                val uuid = UUID.randomUUID().toString()

                val requestObject = JsonObject()
                requestObject.addProperty("id", uuid)
                requestObject.addProperty("method", method)
                requestObject.add("params", params)

                messages
                    .filter { msg -> msg.has("id") }
                    .filter { msg -> msg.get("id").asString == uuid }
                    .firstOrError()
                    .doOnSubscribe { disposable ->
                        val sendSuccess = wsClient.send(RadixJson.gson.toJson(requestObject))
                        if (!sendSuccess) {
                            disposable.dispose()
                            emitter.onError(RuntimeException("Could not connect."))
                        }
                    }
                    .subscribe({ msg ->
                        val received = msg.asJsonObject
                        if (received.has("result")) {
                            emitter.onSuccess(received.get("result"))
                        } else if (received.has("error")) {
                            emitter.onError(RuntimeException(received.toString()))
                        } else {
                            emitter.onError(
                                RuntimeException("Received bad json rpc message: " + received.toString())
                            )
                        }
                    }, { err ->
                        emitter.onError(RuntimeException(err.message))
                    })
            }
        )
    }

    /**
     * Generic helper method for calling a JSON-RPC method with no parameters. Deserializes the received json.
     *
     * @param method name of JSON-RPC method
     * @return response from rpc method
     */
    private fun jsonRpcCall(method: String): Single<JsonElement> {
        return this.jsonRpcCall(method, JsonObject())
    }

    fun getAPIVersion(): Single<Int> {
        return serverApiVersion
    }

    fun checkAPIVersion(): Single<Boolean> {
        return this.getAPIVersion().map(API_VERSION::equals)
    }

    /**
     * Retrieve the universe the node is supporting. The result is cached for future calls.
     *
     * @return universe config which the node is supporting
     */
    fun getUniverse(): Single<RadixUniverseConfig> {
        return this.universeConfig
    }

    /**
     * Connects to this Radix Node if not already connected and queries for an atom by HID.
     * If the node does not carry the atom (e.g. if it does not reside on the same shard) then
     * this method will return an empty Maybe.
     *
     * @param hid the hash id of the atom being queried
     * @return the atom if found, if not, return an empty Maybe
     */
    fun getAtom(hid: EUID): Maybe<Atom> {
        val params = JsonObject()
        params.addProperty("hid", hid.toString())

        return this.jsonRpcCall("Ledger.getAtoms", params)
            .map { result ->
                RadixJson.gson.fromJson<List<Atom>>(result, object : TypeToken<List<Atom>>() {
                }.type)
            }
            .flatMapMaybe { list -> if (list.isEmpty()) Maybe.empty() else Maybe.just(list[0]) }
    }

    /**
     * Generic helper method for creating a subscription via JSON-RPC.
     *
     * @param method name of subscription method
     * @param notificationMethod name of the JSON-RPC notification method
     * @return Observable of emitted subscription json elements
     */
    fun jsonRpcSubscribe(method: String, notificationMethod: String): Observable<JsonElement> {
        return this.jsonRpcSubscribe(method, JsonObject(), notificationMethod)
    }

    /**
     * Generic helper method for creating a subscription via JSON-RPC.
     *
     * @param method name of subscription method
     * @param rawParams parameters to subscription method
     * @param notificationMethod name of the JSON-RPC notification method
     * @return Observable of emitted subscription json elements
     */
    fun jsonRpcSubscribe(method: String, rawParams: JsonObject, notificationMethod: String): Observable<JsonElement> {
        return this.wsClient.connect().andThen(
            Observable.create { emitter ->
                val subscriberId = UUID.randomUUID().toString()
                val params = rawParams.deepCopy()
                params.addProperty("subscriberId", subscriberId)

                val subscriptionDisposable = messages
                    .filter { msg -> msg.has("method") }
                    .filter { msg -> msg.get("method").asString == notificationMethod }
                    .map { msg -> msg.get("params").asJsonObject }
                    .filter { p -> p.get("subscriberId").asString == subscriberId }
                    .subscribe(
                        { emitter.onNext(it) },
                        { emitter.onError(it) }
                    )

                val methodDisposable = this.jsonRpcCall(method, params)
                    .subscribe(
                        { },
                        { emitter.onError(it) }
                    )

                emitter.setCancellable {
                    methodDisposable.dispose()
                    subscriptionDisposable.dispose()

                    val cancelUuid = UUID.randomUUID().toString()
                    val cancelObject = JsonObject()
                    cancelObject.addProperty("id", cancelUuid)
                    cancelObject.addProperty("method", "Subscription.cancel")
                    val cancelParams = JsonObject()
                    cancelParams.addProperty("subscriberId", subscriberId)
                    cancelObject.add("params", cancelParams)
                    wsClient.send(RadixJson.gson.toJson(cancelObject))
                }
            }
        )
    }

    /**
     * Retrieves all atoms from a node specified by a query. This includes all past
     * and future atoms. The Observable returned will never complete.
     *
     * @param atomQuery query specifying which atoms to retrieve
     * @param <T> atom type
     * @return observable of atoms
    </T> */
    fun <T : Atom> getAtoms(atomQuery: AtomQuery<T>): Observable<T> {
        val params = JsonObject()
        params.add("query", atomQuery.toJson())

        return this.jsonRpcSubscribe("Atoms.subscribe", params, "Atoms.subscribeUpdate")
            .map { p -> p.asJsonObject.get("atoms").asJsonArray }
            .flatMapIterable<JsonElement> { array -> array }
            .map<JsonObject> { it.asJsonObject }
            .map { jsonAtom -> RadixJson.gson.fromJson(jsonAtom, atomQuery.atomClass) }
            .map { atom ->
                atom.putDebug("RECEIVED", System.currentTimeMillis())
                atom
            }
    }

    /**
     * Attempt to submit an atom to a node. Returns the status of the atom as it
     * gets stored on the node.
     *
     * @param atom the atom to submit
     * @param <T> the type of atom
     * @return observable of the atom as it gets stored
    </T> */
    fun <T : Atom> submitAtom(atom: T): Observable<AtomSubmissionUpdate> {
        return Observable.create { emitter ->
            val jsonAtom = RadixJson.gson.toJsonTree(atom, Atom::class.java)

            val subscriberId = UUID.randomUUID().toString()
            val params = JsonObject()
            params.addProperty("subscriberId", subscriberId)
            params.add("atom", jsonAtom)

            val subscriptionDisposable = messages
                .filter { msg -> msg.has("method") }
                .filter { msg -> msg.get("method").asString == "AtomSubmissionState.onNext" }
                .map { msg -> msg.get("params").asJsonObject }
                .filter { p -> p.get("subscriberId").asString == subscriberId }
                .map { p ->
                    val state = AtomSubmissionState.valueOf(p.get("value").asString)
                    val message: String?
                    if (p.has("message")) {
                        message = p.get("message").asString
                    } else {
                        message = null
                    }
                    AtomSubmissionUpdate.now(atom.hid, state, message)
                }
                .takeUntil { it.isComplete }
                .subscribe(
                    { emitter.onNext(it) },
                    { emitter.onError(it) },
                    { emitter.onComplete() }
                )

            val methodDisposable = this.jsonRpcCall("Universe.submitAtomAndSubscribe", params)
                .doOnSubscribe {
                    emitter.onNext(AtomSubmissionUpdate.now(atom.hid, AtomSubmissionState.SUBMITTING))
                }
                .subscribe(
                    {
                        emitter.onNext(
                            AtomSubmissionUpdate.now(
                                atom.hid,
                                AtomSubmissionState.SUBMITTED
                            )
                        )
                    },
                    {
                        emitter.onNext(
                            AtomSubmissionUpdate.now(
                                atom.hid,
                                AtomSubmissionState.FAILED,
                                it.message
                            )
                        )
                        emitter.onComplete()
                    }
                )

            emitter.setCancellable {
                methodDisposable.dispose()
                subscriptionDisposable.dispose()
            }
        }
    }

    override fun toString(): String {
        return wsClient.toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RadixJsonRpcClient::class.java)

        /**
         * Betanet does not yet support version checking
         * TODO: this is temporary, remove once supported everywhere
         */
        private val CHECK_API_VERSION = false

        /**
         * API version of Client, must match with Server
         */
        private val API_VERSION = 1
    }
}
