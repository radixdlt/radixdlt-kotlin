package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomValidationException
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.network.AtomQuery
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import com.radixdlt.client.core.network.IncreasingRetryTimer
import com.radixdlt.client.core.network.RadixJsonRpcClient
import com.radixdlt.client.core.network.RadixNetwork
import com.radixdlt.client.core.network.WebSocketClient
import com.radixdlt.client.core.serialization.RadixJson
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import org.slf4j.LoggerFactory
import java.util.HashSet
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A RadixLedger wraps a RadixNetwork and abstracts away network logic providing
 * the main interface to interacting with a Radix Ledger, specifically reading
 * and writing atoms onto the Ledger.
 */
class RadixLedger(
    /**
     * The Universe we need peers for
     * TODO: is this the right place to have this?
     */
    private val config: RadixUniverseConfig,
    val radixNetwork: RadixNetwork
) {

    private val debug = AtomicBoolean(false)
    val magic: Int = config.getMagic()

    fun setDebug(debug: Boolean) {
        this.debug.set(debug)
    }

    /**
     * Maps a public key to it's corresponding Radix myAddress in this universe.
     * Within a universe, a public key has a one to one bijective relationship to an myAddress
     *
     * @param publicKey the key to get an myAddress from
     * @return the corresponding myAddress to the key for this universe
     */
    fun getAddressFromPublicKey(publicKey: ECPublicKey): RadixAddress {
        return RadixAddress(magic, publicKey)
    }

    /**
     * Returns a cold observable of the first peer found which supports
     * a set short shards which intersects with a given set of shards.
     *
     * @param shards set of shards to find an intersection with
     * @return a cold observable of the first matching Radix client
     */
    private fun getRadixClient(shards: Set<Long>): Single<RadixJsonRpcClient> {
        return this.radixNetwork.getRadixClients(shards)
            .flatMapMaybe { client ->
                client.status
                    .filter { status -> status != WebSocketClient.RadixClientStatus.FAILURE }
                    .map { _ -> client }
                    .firstOrError()
                    .toMaybe()
            }
            .flatMapMaybe { client ->
                client.getUniverse()
                    .doOnSuccess { cliUniverse ->
                        if (config != cliUniverse) {
                            LOGGER.warn(
                                "${(client)} has universe: ${cliUniverse.getHash()} but looking for ${config.getHash()}"
                            )
                        }
                    }
                    .map { config == it }
                    .filter { b -> b }
                    .map { _ -> client }
            }
            .firstOrError()
    }

    /**
     * Returns a cold observable of the first peer found which supports
     * a set short shards which intersects with a given shard
     *
     * @param shard a shards to find an intersection with
     * @return a cold observable of the first matching Radix client
     */
    private fun getRadixClient(shard: Long): Single<RadixJsonRpcClient> {
        return getRadixClient(setOf(shard))
    }

    /**
     * Returns a new hot Observable Atom Query which will connect to the network
     * to retrieve the requested atoms.
     *
     * @param destination destination (which determines shard) to query atoms for
     * @param atomClass atom class type to filter for
     * @return a new Observable Atom Query
     */
    fun <T : Atom> getAllAtoms(destination: EUID, atomClass: Class<T>): Observable<T> {
        Objects.requireNonNull(destination)
        Objects.requireNonNull(atomClass)

        val atomQuery = AtomQuery(destination, atomClass)
        return getRadixClient(destination.shard)
            .flatMapObservable { client -> client.getAtoms(atomQuery) }
            .doOnError {
                LOGGER.warn("Error on getAllAtoms $destination")
            }
            .retryWhen(IncreasingRetryTimer())
            .filter(object : Predicate<T> {
                private val atomsSeen = HashSet<RadixHash>()

                override fun test(t: T): Boolean {
                    if (atomsSeen.contains(t.hash)) {
                        LOGGER.warn("Atom Already Seen: destination({}) atom({})", destination, t)
                        return false
                    }
                    atomsSeen.add(t.hash)

                    return true
                }
            })
            .filter { atom ->
                return@filter try {
                    RadixAtomValidator.getInstance().validate(atom)
                    true
                } catch (e: AtomValidationException) {
                    // TODO: Stop stream and mark client as untrustable
                    LOGGER.error(e.toString())
                    false
                }
            }
            .doOnSubscribe {
                LOGGER.info("Atom Query Subscribe: destination({}) class({})", destination, atomClass.simpleName)
            }
            .publish()
            .refCount()
    }

    /**
     * Immediately submits an atom into the ledger without waiting for subscription. The returned
     * observable is a full replay of the status of the atom, from submission to acceptance by
     * the network.
     *
     * @param atom atom to submit into the ledger
     * @return Observable emitting status updates to submission
     */
    fun submitAtom(atom: Atom): Observable<AtomSubmissionUpdate> {
        val status = getRadixClient(atom.requiredFirstShard)
            .doOnSuccess { client -> LOGGER.info("Found client to submit atom: " + client.location) }
            .doOnError { throwable ->
                LOGGER.warn("Error on submitAtom " + atom.hid)
                throwable.printStackTrace()
            }
            .flatMapObservable { client -> client.submitAtom(atom) }
            .doOnError(Throwable::printStackTrace)
            .retryWhen(IncreasingRetryTimer())

        if (debug.get()) {
            try {
                RadixAtomValidator.getInstance().validate(atom)
            } catch (e: AtomValidationException) {
                LOGGER.error(e.toString())
            }

            return status.doOnNext { atomSubmissionUpdate ->
                if (atomSubmissionUpdate.getState() == AtomSubmissionState.VALIDATION_ERROR) {
                    LOGGER.error(atomSubmissionUpdate.message + "\n" + RadixJson.gson.toJson(atom))
                }
            }
        }

        val replay = status.replay()
        replay.connect()

        return replay
    }

    /**
     * Attempt to cleanup resources
     */
    fun close() {
        // TODO: unsubscribe from all atom observables
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RadixLedger::class.java)
    }
}
